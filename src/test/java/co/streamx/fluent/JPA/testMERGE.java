package co.streamx.fluent.JPA;

import static co.streamx.fluent.SQL.AggregateFunctions.SUM;
import static co.streamx.fluent.SQL.Directives.alias;
import static co.streamx.fluent.SQL.Directives.comment;
import static co.streamx.fluent.SQL.Directives.subQuery;
import static co.streamx.fluent.SQL.Directives.viewOf;
import static co.streamx.fluent.SQL.Oracle.SQL.DUAL;
import static co.streamx.fluent.SQL.Oracle.SQL.LOG_ERRORS;
import static co.streamx.fluent.SQL.Oracle.SQL.MERGE;
import static co.streamx.fluent.SQL.Oracle.SQL.MERGE_INSERT;
import static co.streamx.fluent.SQL.Oracle.SQL.MERGE_UPDATE;
import static co.streamx.fluent.SQL.Oracle.SQL.REJECT_LIMIT;
import static co.streamx.fluent.SQL.Oracle.SQL.SYSDATE;
import static co.streamx.fluent.SQL.Oracle.SQL.TO_CHAR;
import static co.streamx.fluent.SQL.Oracle.SQL.UNLIMITED;
import static co.streamx.fluent.SQL.Oracle.SQL.WHEN_MATCHED;
import static co.streamx.fluent.SQL.Oracle.SQL.WHEN_NOT_MATCHED;
import static co.streamx.fluent.SQL.SQL.BY;
import static co.streamx.fluent.SQL.SQL.DELETE;
import static co.streamx.fluent.SQL.SQL.FROM;
import static co.streamx.fluent.SQL.SQL.GROUP;
import static co.streamx.fluent.SQL.SQL.INSERT;
import static co.streamx.fluent.SQL.SQL.SELECT;
import static co.streamx.fluent.SQL.SQL.VALUES;
import static co.streamx.fluent.SQL.SQL.WHERE;
import static co.streamx.fluent.SQL.SQL.row;
import static co.streamx.fluent.SQL.TransactSQL.SQL.$action;
import static co.streamx.fluent.SQL.TransactSQL.SQL.DELETED;
import static co.streamx.fluent.SQL.TransactSQL.SQL.GETDATE;
import static co.streamx.fluent.SQL.TransactSQL.SQL.OUTPUT;
import static co.streamx.fluent.SQL.TransactSQL.SQL.WHEN_MATCHED_AND;
import static co.streamx.fluent.SQL.TransactSQL.SQL.registerVendorCapabilities;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.StopWatch;

import co.streamx.fluent.SQL.Alias;
import co.streamx.fluent.SQL.View;
import co.streamx.fluent.SQL.TransactSQL.MergeAction;
import co.streamx.fluent.notation.Tuple;
import lombok.Data;
import lombok.Getter;

public class testMERGE implements CommonTest {

    @BeforeEach
    public void init() {
        registerVendorCapabilities(FluentJPA::setCapabilities);
    }

    // In real app would be an Entity
    @Tuple
    @Table(name = "ZeroInventory")
    @Getter
    public static class ZeroInventory {
        private int deletedProductID;
        private Date removedOnDate;
    }

    // In real app would be an Entity
    @Tuple
    @Table(name = "ProductInventory")
    @Data
    public static class ProductInventory {
        @OneToOne
        @JoinColumn(name = "product_id")
        private Product product;
        @Column
        private int quantity;
    }

    // In real app would be an Entity
    @Tuple
    @Table(name = "ProductInventory")
    @Data
    public static class Product {
        @Id
        private int productID;
    }

    @Tuple
    @Getter
    public static class Change {
        private int productID;
        private MergeAction action;
    }

    // In real app would be an Entity
    @Tuple
    @Table(name = "SalesOrderDetail")
    @Getter
    public static class SalesOrderDetail {
        private int salesOrderID;
        @ManyToOne
        @JoinColumn(name = "product_id")
        private Product product;
        private int orderQty;
    }

    // In real app would be an Entity
    @Tuple
    @Table(name = "SalesOrderHeader")
    @Getter
    public static class SalesOrderHeader {
        private int salesOrderID;
        private int orderQty;
        private String orderDate;
    }

    @Test
    public void testInsertFromOUTPUT() throws Exception {

        String orderDate = "20070401"; // passed by an argument

        FluentQuery query = FluentJPA.SQL(() -> {

            SalesOrderDetail sales = salesByProducts(orderDate);

            Change change = updateInventoryWithSales(sales);

            trackNoInventory(change);
        });

        String expected = "INSERT   INTO  ZeroInventory (deleted_product_id, removed_on_date)  "
                + "SELECT q2.product_id, GETDATE() "
                + "FROM (MERGE   INTO ProductInventory AS t4  USING (SELECT t0.product_id AS product_id, SUM(t0.order_qty) AS order_qty "
                + "FROM SalesOrderDetail AS t0  INNER JOIN SalesOrderHeader AS t1  ON ((t0.sales_order_id = t1.sales_order_id) AND (t1.order_date = ?1)) "
                + "GROUP BY  t0.product_id  ) AS q1  ON (t4.product_id = q1.product_id) "
                + "WHEN MATCHED AND (t4.quantity <= q1.order_qty)  THEN DELETE   "
                + "WHEN MATCHED   THEN UPDATE   SET quantity = (t4.quantity - q1.order_qty)  "
                + "OUTPUT $action   AS action, DELETED  .product_id AS product_id ) AS q2 "
                + "WHERE (q2.action = 'DELETE')";

        assertQuery(query, expected);
    }

    // the result is SalesOrderDetail since the SELECTed columns are aliased to its fields
    private static SalesOrderDetail salesByProducts(String orderDate) {

        return subQuery((SalesOrderDetail sod,
                         SalesOrderHeader soh) -> {

            // since the function returns SalesOrderDetail, alias
            // SELECTed columns to SalesOrderDetail's fields (type safety is kept)
            Alias<Product> product = alias(sod.getProduct(), SalesOrderDetail::getProduct);
            int orderQty = alias(SUM(sod.getOrderQty()), SalesOrderDetail::getOrderQty);

            SELECT(product, orderQty);
            FROM(sod).JOIN(soh).ON(sod.getSalesOrderID() == soh.getSalesOrderID() && soh.getOrderDate() == orderDate);
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

    @Tuple
    @Table(name = "bonuses")
    @Data
    public static class Bonus {
        @ManyToOne
        @JoinColumn(name = "employee_id")
        private Employee employee;
        private float bonus;
    }

    @Tuple
    @Table(name = "employees")
    @Getter
    public static class Employee {
        @Id
        @Column(name = "employee_id")
        private int id;

        @ManyToOne
        @JoinColumn(name = "department_id")
        private Department department;

        private int salary;
    }

    @Tuple
    @Table(name = "departments")
    @Getter
    public static class Department {
        @Id
        @Column(name = "department_id")
        private int id;

        private String name;
    }

    @Test
    public void testLet1percentBonusForEarners8000() throws Exception {

        int threshold = 8000; // passed by an argument

        FluentQuery query = FluentJPA.SQL((Bonus bonus1) -> {

            Bonus bonus = alias(bonus1, "D");

            Employee empFromDep80 = alias(subQuery((Employee emp) -> {
                SELECT(emp);
                FROM(emp);
                WHERE(emp.getDepartment().getId() == 80);
            }), "S");

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
                MERGE_INSERT(viewOf(bonus, b -> b.getEmployee().getId(), Bonus::getBonus).from(),
                        (Bonus) VALUES(row(empFromDep80.getId(), empFromDep80.getSalary() * .01f)));
                WHERE(empFromDep80.getSalary() <= threshold);
            });
        });

        String expected = "MERGE   INTO bonuses AS D  USING (SELECT t1.* " + "FROM employees AS t1 "
                + "WHERE (t1.department_id = 80) ) AS S  ON (D.employee_id = S.employee_id) "
                + "WHEN MATCHED   THEN UPDATE   SET bonus = (D.bonus + (S.salary * 0.01)) " + "DELETE  "
                + "WHERE (S.salary > ?1) " + "WHEN NOT MATCHED   THEN INSERT(D.employee_id, D.bonus)"
                + "VALUES (S.employee_id, (S.salary * 0.01)) " + "WHERE (S.salary <= ?1)";

        Object[] expectedArgs = { threshold };
        assertQuery(query, expected, expectedArgs);
    }

    @Tuple
    @Table(name = "errlog")
    @Getter
    public static class ErrLog {
    }

    @Test
    public void testSyntax() throws Exception {
        FluentQuery query = FluentJPA.SQL((ErrLog errlog) -> {
            LOG_ERRORS().INTO(errlog).TAG("My bad: " + TO_CHAR(SYSDATE()));
            REJECT_LIMIT(UNLIMITED());
        });

        String expected = "LOG ERRORS   INTO errlog  ( CONCAT( 'My bad: ' ,  TO_CHAR(SYSDATE  ) ) )"
                + "REJECT LIMIT UNLIMITED";

        try {
            assertQuery(query, expected);
        } catch (AssertionError e) {
            expected = "LOG ERRORS INTO errlog ( CONCAT( CONCAT( '' , 'My bad: ' ) , TO_CHAR(SYSDATE ) ) )REJECT LIMIT UNLIMITED";
            assertQuery(query, expected);
        }
    }

    @Tuple
    @Table(name = "member_topic")
    @Data
    public static class MemberTopic /* implements Record3<Integer, Integer, String> */ {
        private int Member;
        private int topic;
        private String notes;
    }

    @Test
    public void testUpsert() throws Exception {
        FluentQuery query = FluentJPA.SQL((MemberTopic target) -> {

            MemberTopic values = (MemberTopic) VALUES(row(0, 110, "test"));

            // in SQL Server source is a view (i.e. column declaration comes after alias)
            View<MemberTopic> source = viewOf(values, MemberTopic::getMember, MemberTopic::getTopic,
                    MemberTopic::getNotes);

            // match new record with existing
            MERGE().INTO(target)
                    .USING(source)
                    // matching criteria
                    .ON(target.getMember() == values.getMember() && target.getTopic() == values.getTopic());

            WHEN_MATCHED().THEN(() -> {
                MERGE_UPDATE().SET(() -> {
                    target.setNotes(values.getNotes());
                });
            });

            WHEN_NOT_MATCHED().THEN(MERGE_INSERT(source.columnNames(), VALUES(source.from())));

            comment(VALUES(source.from(values)));
            comment(VALUES(source.from(target)));

        });

        String expected = "MERGE   INTO member_topic AS t0  USING  (VALUES (0, 110, 'test') ) AS t1 (member, topic, notes)   ON ((t0.member = t1.member) AND (t0.topic = t1.topic)) "
                + "WHEN MATCHED   THEN UPDATE   SET notes = t1.notes  "
                + "WHEN NOT MATCHED   THEN INSERT (member, topic, notes) VALUES (t1.member, t1.topic, t1.notes)   "
                + "-- VALUES (t1.member, t1.topic, t1.notes)  " + "-- VALUES (t0.member, t0.topic, t0.notes)";

        assertQuery(query, expected);

    }

    @Test
    public void testUpsertSQL2() throws Exception {
        FluentQuery query = FluentJPA.SQL((MemberTopic target) -> {

            // view is used for INSERT and alias
            View<MemberTopic> targetView = viewOf(target, MemberTopic::getMember, MemberTopic::getTopic,
                    MemberTopic::getNotes);

            // source is a sub query
            MemberTopic source = subQuery(() -> {
                // alias values in the view order
                SELECT(targetView.alias(0, 110, "test"));
            });

            // match new record with existing
            MERGE().INTO(target)
                    .USING(source)
                    // matching criteria
                    .ON(target.getMember() == source.getMember() && target.getTopic() == source.getTopic());

            WHEN_MATCHED().THEN(() -> {
                MERGE_UPDATE().SET(() -> {
                    target.setNotes(source.getNotes());
                });
            });

            WHEN_NOT_MATCHED().THEN(MERGE_INSERT(targetView.columnNames(), VALUES(targetView.from(source))));

        });

        String expected = "MERGE   INTO member_topic AS t0  USING (SELECT  0 AS member, 110 AS topic, 'test' AS notes  ) AS q0  ON ((t0.member = q0.member) AND (t0.topic = q0.topic)) "
                + "WHEN MATCHED   THEN UPDATE   SET notes = q0.notes  "
                + "WHEN NOT MATCHED   THEN INSERT (member, topic, notes) VALUES (q0.member, q0.topic, q0.notes)";

        assertQuery(query, expected);

    }

    @Test
    public void testUpsertOracle() throws Exception {

        co.streamx.fluent.SQL.Oracle.SQL.registerVendorCapabilities(FluentJPA::setCapabilities);

        StopWatch stopper = new StopWatch();

        while (stopper.getTotalTimeSeconds() < 1000) {
            MemberTopic x = new MemberTopic();
            x.setMember(5);
            x.setTopic(4);

            stopper.start();
            FluentQuery query = FluentJPA.SQL((MemberTopic target) -> {

                // view is used for INSERT and alias
                View<MemberTopic> targetView = viewOf(target, MemberTopic::getMember, MemberTopic::getTopic,
                        MemberTopic::getNotes);

                // source is a sub query
                MemberTopic source = subQuery(() -> {
                    SELECT(targetView.fromAliased(x, "test"));
                    FROM(DUAL()); // Must be in Oracle
                });

                // match new record with existing
                MERGE().INTO(target)
                        .USING(source)
                        // matching criteria
                        .ON(target.getMember() == source.getMember() && target.getTopic() == source.getTopic());

                WHEN_MATCHED().THEN(() -> {
                    MERGE_UPDATE().SET(() -> {
                        target.setNotes(source.getNotes());
                    });
                });

                WHEN_NOT_MATCHED()
                        .THEN(MERGE_INSERT(targetView.columnNames(), VALUES(targetView.from(source, "override"))));

            });

            stopper.stop();
            System.out.println(stopper.getLastTaskTimeMillis());

            String expected = "MERGE   INTO member_topic t0  USING (SELECT  ?1 AS member, ?2 AS topic, 'test' AS notes  "
                    + "FROM DUAL   ) q0  ON ((t0.member = q0.member) AND (t0.topic = q0.topic)) "
                    + "WHEN MATCHED   THEN UPDATE   SET notes = q0.notes  "
                    + "WHEN NOT MATCHED   THEN INSERT (member, topic, notes) VALUES (q0.member, q0.topic, 'override')";

            assertQuery(query, expected);

//            Thread.sleep(1000);
            break;
        }


    }
}
