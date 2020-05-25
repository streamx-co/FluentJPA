# Get Back in Control of Your SQL with JPA <div style="float:right">![Patent Pending](https://img.shields.io/badge/patent-pending-informational) [![License](https://img.shields.io/badge/license-LGPL_3.0-success)](LICENSE) ![Java Version](https://img.shields.io/badge/java-%3E%3D%208-success) [![Build Status](https://travis-ci.org/streamx-co/FluentJPA.svg?branch=master)](https://travis-ci.org/streamx-co/FluentJPA) [![Maven Central](https://img.shields.io/maven-central/v/co.streamx.fluent/fluent-jpa?label=maven%20central)](https://search.maven.org/search?q=g:%22co.streamx.fluent%22%20AND%20a:%22fluent-jpa%22)</div>

FluentJPA is a Language Integrated Query (LINQ) technology for relational (SQL) databases and JPA. It allows you to use Java to write strongly typed queries by directly integrating into the language.

## How does FluentJPA integrate into Java?

At first glance, it seems that we need a hook in the Java compiler. But in fact, we have full access to the compiled bytecode, which has all the necessary "knowledge". This is how FluentJPA does its magic - it reads the bytecode and translates it to SQL.

As a result, the integration is full, and FluentJPA supports all Java language constructs, including functions, variables, etc - anything the compiler can compile and also makes sense in the SQL context. See [Java Language Support](https://github.com/streamx-co/FluentJPA/wiki/Java-Language-Support) for details.

## We already have JPA, JPA repositories and other technologies

> FluentJPA seeks to complement JPA where the developer wants to gain control over SQL

FluentJPA declares SQL clauses (like `SELECT`, `FROM`, `WHERE`) as first class Java methods, so the queries are visually similar:

```java
// Java
FluentJPA.SQL((Person p) -> {
    SELECT(p);
    FROM(p);
    WHERE(p.getName() == name);
});
```

```SQL
-- SQL
SELECT t0.*
FROM PERSON_TABLE t0
WHERE (t0.name = ?)
```

As a result, using FluentJPA you can write SQL without loss of type safety, intellisense, refactoring.

## JPA Integration

FluentJPA reads JPA annotations to map entities to SQL table names and properties to column names. Then it uses JPA native query for execution. As a result the solution integrates with JPA pipeline and transactions, calls to JPA and FluentJPA can be mixed freely giving the correct results.

## SQL Support

FluentJPA supports the entire **modern** SQL [DML](https://en.wikipedia.org/wiki/Data_manipulation_language) standard. In addition to SQL-92, where JPQL lives, FluentJPA supports [SQL-99 Common Table Expressions](https://github.com/streamx-co/FluentJPA/wiki/Common-Table-Expressions) (`WITH` clause), [SQL-2003 Window Functions](https://github.com/streamx-co/FluentJPA/wiki/Window-Functions) (`OVER` clause), [SQL-2003 MERGE](https://github.com/streamx-co/FluentJPA/wiki/MERGE) (`UPSERT` clause), [Dynamic Queries](https://github.com/streamx-co/FluentJPA/wiki/Dynamic-Queries) without [Criteria API](https://en.wikibooks.org/wiki/Java_Persistence/Criteria) and many, many more.

FluentJPA also supports proprietary SQL extensions provided by the 4 most popular databases, see [static imports](https://github.com/streamx-co/FluentJPA/wiki/Setup#static-imports). Follow links in **Basic/Advanced SQL DML Statements** from the [wiki](https://github.com/streamx-co/FluentJPA/wiki) sidebar to see examples.

* All functions mapped to SQL counterparts follow SQL naming convention - capitals with underscores as delimiters. As a result your code looks like SQL, but *is* Java with intellisense and compiler validation!
* All helper functions follow standard Java naming convention. They are either [Library](https://github.com/streamx-co/FluentJPA/wiki/Library) methods or [Directives](https://github.com/streamx-co/FluentJPA/wiki/Directives).

<!-- ## Is it same as [Microsoft LINQ](https://docs.microsoft.com/en-us/dotnet/csharp/programming-guide/concepts/linq/)?

Both technologies let write queries in the platform language (Java, C#) in the most natural way, benefit from intellisense, refactoring and type safety.

But there are also fundamental differences:

- MS LINQ is not similar to SQL, approach that has its pluses and minuses.

-->

## FluentJPA.SQL()

This is an "entry-point" method to the FluentJPA. It accepts a Java lambda and translates it to SQL query. There are few conventions:

* Lambda parameters must be entity types. This way we **declare** the *table references* to be used in this query. Like in SQL, if there is a self join, there will be 2 parameters of the same entity type. For example:

  ```java
  FluentQuery query = FluentJPA.SQL((Staff emp,
                                     Staff manager,
                                     Store store) -> {
      // returns store name, employee first name and its manager first name
      // ordered by store and manager
      SELECT(store.getName(), emp.getFirstName(), manager.getFirstName());
      FROM(emp).JOIN(manager).ON(emp.getManager() == manager)
               .JOIN(store).ON(emp.getStore() == store);
      ORDER(BY(emp.getStore()), BY(emp.getManager()));

  });
  ```

  > * In Java entity represents SQL Table or more generally a *column set*
  > * Having entities as parameters makes clear which tables this query works on

* Every time, where SQL expects a table reference (e.g. `FROM`), an entity should be passed. FluentJPA will read the required Table information via JPA annotations.

* FluentJPA translates Lambda's body SQL clauses (written in Java) in the same order as they appear. Thus the content of the sample above is translated to exactly 3 lines:

  ```sql
  SELECT t2.store_name, t0.first_name, t1.first_name
  FROM staffs AS t0 INNER JOIN staffs AS t1 ON (t0.manager_id = t1.staff_id) INNER JOIN stores AS t2 ON (t0.store_id = t2.store_id)
  ORDER BY t0.store_id, t0.manager_id
  ```

* Finally, call `FluentQuery.createQuery()` to get a standard JPA Query instance (see [JPA Integration](https://github.com/streamx-co/FluentJPA/wiki/JPA-Integration) for details):

  ```java
  TypedQuery<X> typedQuery = query.createQuery(entityManager, <X>.class);
  // execute the query
  typedQuery.getResultList(); // or getSingleResult() / executeUpdate()
  ```

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

> This work is dual-licensed under [Affero GPL 3.0](https://opensource.org/licenses/AGPL-3.0) and [Lesser GPL 3.0](https://opensource.org/licenses/LGPL-3.0).
The source code is licensed under AGPL and [official binaries](https://search.maven.org/search?q=g:%22co.streamx.fluent%22%20AND%20a:%22fluent-jpa%22) under LGPL.

Therefore the library can be used in commercial projects.

`SPDX-License-Identifier: AGPL-3.0-only AND LGPL-3.0-only`
