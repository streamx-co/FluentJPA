# Get Back in Control of Your SQL with JPA <div style="float:right">![Patent Pending](https://img.shields.io/badge/patent-pending-informational) [![License](https://img.shields.io/github/license/streamx-co/fluent-jpa?color=green)](LICENSE) ![Java Version](https://img.shields.io/badge/java-%3E%3D%208-success) [![Build Status](https://travis-ci.org/streamx-co/FluentJPA.svg?branch=master)](https://travis-ci.org/streamx-co/FluentJPA) [![Maven Central](https://img.shields.io/maven-central/v/co.streamx.fluent/fluent-jpa?label=maven%20central)](https://search.maven.org/search?q=g:%22co.streamx.fluent%22%20AND%20a:%22fluent-jpa%22)</div>

<!-- [commercial usage](https://fluentjpa.com) -->

JPA is a great technology that maps the database relational model to the Java object oriented model. It lets retrieve data and persist back the changes very easily. But it lacks the ability to perform advanced queries. In fact, all the advanced SQL capabilities are simply locked to the Java developer until she chooses to write a hard-to-maintain SQL as a hard-coded string.

FluentJPA fills this gap in three ways:

* allows you to use Java to write strongly typed queries. We support operators, parameters, variables, methods, etc and translate them to the SQL.
* naturally extends JPA: use entities in FROM clauses, getters and setters to write expressions, store intermediate calculations in variables, pass them to methods as you usually do to program your business logic. FluentJPA reads all the JPA annotations to retrieve column and table names, then it uses JPA native query for execution. As a result the solution is integrated with JPA pipeline and transaction, calls to JPA and FluentJPA can be freely mixed producing correct results.
* covers the entire **modern** SQL [DML](https://en.wikipedia.org/wiki/Data_manipulation_language) standard. SQL has changed since SQL-92, where JPQL is stuck: [SQL-99 Common Table Expressions](https://github.com/streamx-co/FluentJPA/wiki/Common-Table-Expressions) (`WITH` clause), [SQL-2003 Window Functions](https://github.com/streamx-co/FluentJPA/wiki/Window-Functions) (`OVER` clause), [SQL-2003 MERGE](https://github.com/streamx-co/FluentJPA/wiki/MERGE) (`UPSERT` clause), [Dynamic Queries](https://github.com/streamx-co/FluentJPA/wiki/Dynamic-Queries) without [Criteria API](https://en.wikibooks.org/wiki/Java_Persistence/Criteria) and many, many more. FluentJPA offers this power as a handy Java library.

## Competition

How is FluentJPA different (better) than its competitors, jOOQ in particular? This question [was asked](https://www.reddit.com/r/java/comments/ctu53m/fluentjpa_provides_fluent_api_for_writing/exr9g1t) on Reddit by Mr. Lukas Eder, CEO of Data Geekery GmbH, the developer of jOOQ. Here is [the answer](https://github.com/streamx-co/FluentJPA/wiki/Solving-real-problems), showing how FluentJPA solves real problems.

## Setup

There is no bootstrap, code generation step or anything else needed to use FluentJPA. Add [dependencies](https://github.com/streamx-co/FluentJPA/wiki/Setup) to your project enjoy the type-safe Object Oriented SQL in your JPA project without compromises! (Disclaimer: FluentJPA strives to be as unobtrusive as possible. We don't change or affect anything, so your existing code will continue to work as before. We don't bring **any** dependencies except our own code and [ASM](https://asm.ow2.io/), total ~500K).

* [Setup Instructions](https://github.com/streamx-co/FluentJPA/wiki/Setup)
* [Documentation & Tutorials](https://github.com/streamx-co/FluentJPA/wiki)

## Usage Examples

<!-- > Probably the most important feature missing in JPA is **Sub Query**. Joins are kind of "solved" by JPA annotations combined with JPA loading policies. With JPA Repositories [Query Creation](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#repositories.query-methods.query-creation) it's possible to easily create most of single queries required in practice. But there is absolutely no good solution when a sub query is needed! Not only FluentJPA supports sub queries very well, it also lets put them into a separate Java function, for brevity. After all, sub query is sort of a lambda inside an SQL query, I think. -->
> Probably the most important feature missing in JPA is **Sub Query**. We think that any serious SQL starts with them (just look [here](https://mode.com/sql-tutorial/sql-subqueries/) for few examples). Not only FluentJPA supports sub queries, it also lets put them into a separate Java(!) function. So the code looks 100% natural to a Java developer.

Let's start with the simplest query possible to overview the entire flow. (A bit spiced with passing an external parameter and optional JPA Repository integration)

### Example 0 - [testPassArguments()](src/test/java/co/streamx/fluent/JPA/repository/PersonRepository.java#L24-L40)

```java
@Repository
public interface PersonRepository extends CrudRepository<Person, Long>, EntityManagerSupplier {

    default List<Person> getAllByName(String name) {
        FluentQuery query = FluentJPA.SQL((Person p) -> {
            SELECT(p);
            FROM(p);
            WHERE(p.getName() == name);
        });

        return query.createQuery(getEntityManager(), Person.class).getResultList();
    }
}
```

SQL query that gets generated, `name` is passed via a parameter:

```SQL
SELECT t0.*
FROM PERSON_TABLE t0
WHERE (t0.name = ?)
```

FluentJPA supports _**any**_ query, here we brought few examples with sub queries to show the power of FluentJPA. There is a link to the test file source code and a link to the original SQL where we borrowed the use case from. Best when seen side-by-side.

### Example 1 - [testCorrelatedWithHaving()](src/test/java/co/streamx/fluent/JPA/testSELECT.java#L161-L186)

**1 sub query "converted" to a Java function** (original SQL comes from [SQL Server documentation](https://docs.microsoft.com/en-us/sql/t-sql/queries/select-examples-transact-sql?view=sql-server-2017#e-using-correlated-subqueries)).

> Citing original docs: *This example finds the product models for which the maximum list price is more than twice the average for the model.*

```java
// Product is a standard JPA Entity
FluentQuery query = FluentJPA.SQL((Product p1) -> {

    SELECT(p1.getModel());
    FROM(p1);
    GROUP(BY(p1.getModel()));
    HAVING(MAX(p1.getListPrice()) >= ALL(avgPriceForProductModel(p1.getModel())));
    // sub query in SQL, function in Java ^^^^^^^^^^^^^^^^^^^^
});

...

// The result is an int since the sub query returns 1 row/column
private static int avgPriceForProductModel(ProductModel model) {
    return subQuery((Product p2) -> {
        SELECT(AVG(p2.getListPrice()));
        FROM(p2);
        WHERE(model == p2.getModel());
    });
}
```

### Example 2 - [testInsertFromOUTPUT()](src/test/java/co/streamx/fluent/JPA/testMERGE.java#L124-L198)

**3 sub queries "converted" to functions** (original SQL comes from [SQL Server documentation](https://docs.microsoft.com/en-us/sql/t-sql/queries/output-clause-transact-sql#k-inserting-data-returned-from-an-output-clause)).

```java
// Arguments are automatically captured and passed in via JPA's Query.setParameter()
String orderDate; // passed by an external parameter

FluentQuery query = FluentJPA.SQL(() -> {

    // returns an entity!
    SalesOrderDetail sales = salesByProducts(orderDate);

    // previous result is an argument for the next function
    Change change = updateInventoryWithSales(sales);

    trackNoInventory(change);
});

...

// the result is SalesOrderDetail since the SELECTed columns are aliased to its fields
private static SalesOrderDetail salesByProducts(String orderDate) {

    return subQuery((SalesOrderDetail sod,
                        SalesOrderHeader soh) -> {

        // since the function returns SalesOrderDetail, alias
        // SELECTed columns to SalesOrderDetail's fields (type safety is kept)
        Product product = alias(sod.getProduct(), SalesOrderDetail::getProduct);
        int orderQty = alias(SUM(sod.getOrderQty()), SalesOrderDetail::getOrderQty);

        SELECT(product, orderQty);
        FROM(sod).JOIN(soh)
                 .ON(sod.getSalesOrderID() == soh.getSalesOrderID() && soh.getOrderDate() == orderDate);
        GROUP(BY(product));
    });
}

private static Change updateInventoryWithSales(SalesOrderDetail order) {

    return subQuery((ProductInventory inv) -> {

        ProductInventory deleted = DELETED();

        MERGE().INTO(inv).USING(order).ON(inv.getProduct() == order.getProduct());
        // Non foreign key Object JOIN -----------------^^^^^^^^

        WHEN_MATCHED_AND(inv.getQuantity() - order.getOrderQty() <= 0).THEN(DELETE());

        WHEN_MATCHED().THEN(MERGE_UPDATE().SET(() -> {
            inv.setQuantity(inv.getQuantity() - order.getOrderQty());
        }));

        // since the function returns Change, alias
        // OUTPUTed columns to Change's fields
        MergeAction action = alias($action(), Change::getAction);
        int productID = alias(deleted.getProduct().getProductID(), Change::getProductID);
        OUTPUT(action, productID);
    });
}

private static void trackNoInventory(Change change) {

    subQuery((ZeroInventory zi) -> {

        INSERT().INTO(viewOf(zi, ZeroInventory::getDeletedProductID, ZeroInventory::getRemovedOnDate));

        SELECT(change.getProductID(), GETDATE());
        FROM(change);
        WHERE(change.getAction() == MergeAction.DELETE);
    });
}

```

### Example 3 - [testCTE_Recursive_DELETE()](src/test/java/co/streamx/fluent/SQL/TestSQL.java#L443-L468)

**Recursive sub query** (original SQL comes from [PostgreSQL documentation](https://www.postgresql.org/docs/current/queries-with.html)).

> Citing original docs: *This query would remove all direct and indirect subparts of a product.*  
> If you don't know what recursive `WITH` is, it's worth learning. Since you will be *able* to use it now with FluentJPA :wink:.

```java
FluentJPA.SQL((Part allParts) -> {

    Part included_parts = subQuery((Part it,
                                    Part parts,
                                    Part subParts) -> {
        // initial
        SELECT(parts.getSubPart(), parts.getName());
        FROM(parts);
        WHERE(parts.getName() == "our_product");

        UNION_ALL();

        // recursive
        SELECT(subParts.getSubPart(), subParts.getName());

        // recurse
        FROM(recurseOn(it), subParts);
        WHERE(it.getSubPart() == subParts.getName());
    });

    WITH(RECURSIVE(included_parts));

    DELETE().FROM(allParts);
    WHERE(collect(included_parts, included_parts.getName()).contains(allParts.getName()));

});
```

### Example 4 - [getByNameLike()](src/test/java/co/streamx/fluent/JPA/GrammarTest.java#L295-L316)

[Dynamic Queries](https://github.com/streamx-co/FluentJPA/wiki/Dynamic-Queries) without [Criteria API](https://en.wikibooks.org/wiki/Java_Persistence/Criteria):

```java
// build the criteria dynamically
Function1<CoverageMaster, Boolean> dynamicFilter = buildOr1(likes);

FluentQuery query = FluentJPA.SQL((UtilizationDTL util,
                                   UtilizationCoverageDTL utilizationCover,
                                   CoverageMaster coverMaster) -> {
    SELECT(DISTINCT(util.getId()));
    FROM(util).JOIN(utilizationCover)
              .ON(utilizationCover.getUtilization() == util)
              .JOIN(coverMaster)
              .ON(utilizationCover.getMaster() == coverMaster);

    WHERE(dynamicFilter.apply(coverMaster) && util.isCompleted());
      //  ^^^^^^^^^^^^^^^^^^^--- inject the criteria,
      //                         rest of the query is unaffected
    ORDER(BY(util.getId()));
});

private Function1<CoverageMaster, Boolean> buildOr1(List<String> likes) {
    Function1<CoverageMaster, Boolean> criteria = Function1.FALSE();

    for (String like : likes)
        criteria = criteria.or(p -> p.getCoverageName().toLowerCase()
                                                       .matches(parameter(like)));

    return criteria;
}
```

> [Full documentation](https://github.com/streamx-co/FluentJPA/wiki)

## License

Fluent JPA employs a Freemium license model, where some functionality is totally free and more advanced features [require a license](https://fluentjpa.com). Data retrieval expressions (SELECT), including CTE, Window Functions, Aggregate Expressions are free. Data update expressions (INSERT/UPDATE/DELETE/MERGE) and Dynamic Queries [require a license](https://fluentjpa.com).

> **Note for Forkers**: this library depends on [ExTree library](https://github.com/streamx-co/ExTree), which is licensed under [AGPL-3.0](https://github.com/streamx-co/ExTree/blob/master/LICENSE). It has a special licensing exception for official `Streamx` artefacts only. As a result, any fork of this library must be licensed under AGPL license.
