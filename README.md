# FluentJPA <div style="float:right">![Patent Pending](https://img.shields.io/badge/patent-pending-informational) ![License](https://img.shields.io/github/license/streamx-co/fluent-jpa) ![Java Version](https://img.shields.io/badge/java-%3E%3D%208-success) [![Build Status](https://travis-ci.org/streamx-co/FluentJPA.svg?branch=master)](https://travis-ci.org/streamx-co/FluentJPA) [![Maven Central](https://img.shields.io/maven-central/v/co.streamx.fluent/fluent-jpa?label=maven%20central)](https://search.maven.org/search?q=g:%22co.streamx.fluent%22%20AND%20a:%22fluent-jpa%22)</div>

## Fluent API for writing type-safe SQL queries with JPA

> [Full documentation](https://github.com/streamx-co/FluentJPA/wiki)

JPA is a great technology that maps the database relational model to the Java object oriented model. It lets retrieve data and persist back the changes very easily. But it lacks the ability to perform advanced queries. In fact, all the advanced SQL capabilities are simply locked to the Java developer until she chooses to write a hard-to-maintain SQL as a hard-coded string...

FluentJPA project aims to fill this gap in two ways:

* lets writing native SQL in **Java**!
And by saying Java, we **mean** Java. There is no DSL or semantic gap. You use `+` for addition and `-` for subtraction. You use getter to get a property value and setter to set it. You use functions and variables, so when you call SQL `SELECT`, you call it like any other library method. And when you need a sub query, you will probably prefer to put it in a separate function ... as you usually do when you code rest of your business logic. To accomplish this, FluentJPA reads the Java Byte Code from the .class files in runtime and translates it all the way to SQL.
* naturally extending the JPA model. Once you mapped your entities, forget about mapping. Use JPA entity getters and setters to write expressions and joins, store intermediate calculations in variables, pass them to methods - we seamlessly translate it to SQL.

There is no bootstrap, code generation step or anything else needed to use FluentJPA. Add [dependencies](https://github.com/streamx-co/FluentJPA/wiki/Setup) to your project build system and unlock the full power of type-safe Object Oriented SQL in your JPA project without compromises!

<!-- > Probably the most important feature missing in JPA is **Sub Query**. Joins are kind of "solved" by JPA annotations combined with JPA loading policies. With JPA Repositories [Query Creation](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#repositories.query-methods.query-creation) it's possible to easily create most of single queries required in practice. But there is absolutely no good solution when a sub query is needed! Not only FluentJPA supports sub queries very well, it also lets put them into a separate Java function, for brevity. After all, sub query is sort of a lambda inside an SQL query, I think. -->
> Probably the most important feature missing in JPA is **Sub Query**. We think that any serious SQL starts with them (just look [here](https://mode.com/sql-tutorial/sql-subqueries/) for few examples). Not only FluentJPA supports sub queries, it also lets put them into a separate Java(!) function. So the code looks 100% natural to a Java developer.

Let's start easy and look on the simplest query possible and understand the entire flow. (A bit spiced with passing an external parameter and optional JPA Repository integration)

### Example 0 - [testPassArguments()](src/test/java/co/streamx/fluent/JPA/PersonRepositoryTest.java)

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

FluentJPA supports _**any**_ query, here we brought few examples with sub queries to show the power of FluentJPA. There is a link to the test file source code labeled as a method name above each example. And a link to the original SQL where we borrowed the use case from. Best when seen side-by-side.

### Example 1 - [testCorrelatedWithHaving()](src/test/java/co/streamx/fluent/JPA/testSELECT.java)

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

### Example 2 - [testInsertFromOUTPUT()](src/test/java/co/streamx/fluent/JPA/testMERGE.java)

**3 sub queries "converted" to functions** (original SQL comes from [SQL Server documentation](https://docs.microsoft.com/en-us/sql/t-sql/queries/output-clause-transact-sql?view=sql-server-2017#k-inserting-data-returned-from-an-output-clause)).

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

### Example 3 - [testCTE_Recursive_DELETE()](src/test/java/co/streamx/fluent/JPA/TestSQL.java)

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

### Example 4 - [testLet1percentBonusForEarners8000()](src/test/java/co/streamx/fluent/JPA/testMERGE.java)

**1 sub query and a very nice MERGE clause** (original SQL comes from [Oracle documentation](https://docs.oracle.com/en/database/oracle/oracle-database/18/sqlrf/MERGE.html), first example)

```java
int threshold; // passed by an external parameter

FluentQuery query = FluentJPA.SQL((Bonus bonus1) -> {

    Bonus bonus = alias(bonus1, "D");

    Employee empFromDep80 = alias(employeesFromDepartment(80), "S");

    MERGE().INTO(bonus).USING(empFromDep80).ON(bonus.getEmployee() == empFromDep80);
    // Uses @JoinColumn to resolve the association --------------^^^^^^

    WHEN_MATCHED().THEN(() -> {
        MERGE_UPDATE().SET(() -> {
            bonus.setBonus(bonus.getBonus() + empFromDep80.getSalary() * .01f);
        });

        DELETE();
        WHERE(empFromDep80.getSalary() > threshold);
    });

    WHEN_NOT_MATCHED().THEN(() -> {
        MERGE_INSERT(bonus.getEmployee(), bonus.getBonus());
        VALUES(row(empFromDep80.getId(), empFromDep80.getSalary() * .01f));
        WHERE(empFromDep80.getSalary() <= threshold);
    });
});

private static Employee employeesFromDepartment(int num) {
    return subQuery((Employee emp) -> {
        SELECT(emp);
        FROM(emp);
        WHERE(emp.getDepartment().getId() == num);
    });
}
```

> [Full documentation](https://github.com/streamx-co/FluentJPA/wiki)
