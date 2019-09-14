package co.streamx.fluent.JPA;

import static co.streamx.fluent.SQL.AggregateFunctions.AVG;
import static co.streamx.fluent.SQL.AggregateFunctions.MAX;
import static co.streamx.fluent.SQL.AggregateFunctions.SUM;
import static co.streamx.fluent.SQL.Directives.aggregateBy;
import static co.streamx.fluent.SQL.Directives.alias;
import static co.streamx.fluent.SQL.Directives.byRef;
import static co.streamx.fluent.SQL.Directives.subQuery;
import static co.streamx.fluent.SQL.Library.COUNT;
import static co.streamx.fluent.SQL.Library.collectRows;
import static co.streamx.fluent.SQL.Library.pick;
import static co.streamx.fluent.SQL.Library.pickRow;
import static co.streamx.fluent.SQL.Library.selectAll;
import static co.streamx.fluent.SQL.Library.selectMany;
import static co.streamx.fluent.SQL.Operators.ALL;
import static co.streamx.fluent.SQL.Operators.EXISTS;
import static co.streamx.fluent.SQL.Operators.less;
import static co.streamx.fluent.SQL.Oracle.SQL.registerVendorCapabilities;
import static co.streamx.fluent.SQL.SQL.BY;
import static co.streamx.fluent.SQL.SQL.DISTINCT;
import static co.streamx.fluent.SQL.SQL.FROM;
import static co.streamx.fluent.SQL.SQL.GROUP;
import static co.streamx.fluent.SQL.SQL.HAVING;
import static co.streamx.fluent.SQL.SQL.ORDER;
import static co.streamx.fluent.SQL.SQL.ROW;
import static co.streamx.fluent.SQL.SQL.SELECT;
import static co.streamx.fluent.SQL.SQL.WHERE;
import static co.streamx.fluent.SQL.SQL.WITH;
import static co.streamx.fluent.SQL.SQL.row;
import static co.streamx.fluent.SQL.TransactSQL.SQL.NEXT_VALUE_FOR;
import static co.streamx.fluent.SQL.TransactSQL.SQL.sequence;

import java.sql.Date;
import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import co.streamx.fluent.SQL.DataType;
import co.streamx.fluent.SQL.PostgreSQL.DataTypeNames;
import co.streamx.fluent.SQL.PostgreSQL.DataTypes;
import co.streamx.fluent.SQL.TransactSQL.Sequence;
import co.streamx.fluent.notation.Tuple;
import lombok.Data;
import lombok.Getter;

public class testSELECT implements CommonTest, ElementCollectionTypes {

    @BeforeAll
    public static void init() {
        registerVendorCapabilities(FluentJPA::setCapabilities);
    }

    @Tuple
    @Data
    @Table(name = "employees")
    public static class Employee {
        @Id
        private int employeeId;
        private int salary;
        private float commissionPct;

        @ManyToOne
        @JoinColumn(name = "department_id")
        private Department department;
    }

    @Tuple
    @Data
    @Table(name = "departments")
    public static class Department {
        @Id
        private int departmentId;
        private String departmentName;
    }

    @Tuple
    @Getter
    public static class DeptCost {
        private String departmentName;
        private int deptTotal;
    }

    @Tuple
    @Getter
    public static class AvgCost {
        private int avg;
    }

    @Test
    public void testSelectSubqueryFactoring() throws Exception {

        FluentQuery query = FluentJPA.SQL(() -> {

            DeptCost deptCost = subQuery((Employee e,
                                          Department d) -> {
                Integer deptTotal = alias(SUM(e.getSalary()), DeptCost::getDeptTotal);
                String deptName = alias(d.getDepartmentName(), DeptCost::getDepartmentName);

                SELECT(deptName, deptTotal);
                FROM(e, d);
                WHERE(e.getDepartment() == d);
                GROUP(BY(deptName));
            });

            AvgCost avgCost = subQuery(() -> {

                Integer avg = alias(SUM(deptCost.getDeptTotal()) / COUNT(), AvgCost::getAvg);

                SELECT(avg);
                // at this point FluentJPA is unaware that deptCost will be declared
                // using WITH, and without byRef() will generate a sub select
                FROM(byRef(deptCost));
            });

            WITH(deptCost, avgCost);

            selectAll(deptCost);
            WHERE(deptCost.getDeptTotal() > pick(avgCost, avgCost.getAvg()));
            ORDER(BY(deptCost.getDepartmentName()));

        });

        String expected = "WITH q1 AS " + "(SELECT t1.department_name AS department_name, SUM(t0.salary) AS dept_total "
                + "FROM employees t0, departments t1 " + "WHERE (t0.department_id = t1.department_id) "
                + "GROUP BY  t1.department_name  )" + ",q2 AS " + "(SELECT (SUM(q1.dept_total) / COUNT(*)) AS avg "
                + "FROM q1 )" + "SELECT q1.* " + "FROM q1 " + "WHERE (q1.dept_total > (SELECT q2.avg " + "FROM q2 )) "
                + "ORDER BY  q1.department_name";

        assertQuery(query, expected);
    }

    @Tuple
    @Data
    @Table(name = "Product", schema = "Production")
    public static class Product {
        @ManyToOne
        @JoinColumn(name = "ProductModelID")
        private ProductModel model;
        private int listPrice;
        private String name;
    }

    @Tuple
    @Data
    @Table(name = "Model", schema = "Production")
    public static class ProductModel {
        @Id
        @Column(name = "ProductModelID")
        private int id;
        private String name;
    }

    @Test
    public void testCorrelatedWithHaving() throws Exception {

        FluentQuery query = FluentJPA.SQL((Product p1) -> {

            SELECT(p1.getModel());
            FROM(p1);
            GROUP(BY(p1.getModel().getId()));
            HAVING(MAX(p1.getListPrice()) >= ALL(avgPriceForProductModel(p1.getModel())));

        });

        String expected = "SELECT t0.ProductModelID " + "FROM Production.Product t0 "
                + "GROUP BY  t0.ProductModelID  " + "HAVING (MAX(t0.list_price) >= ALL(SELECT AVG(t1.list_price) "
                + "FROM Production.Product t1 " + "WHERE (t0.ProductModelID = t1.ProductModelID) ))";

        assertQuery(query, expected);
    }

    private static int avgPriceForProductModel(ProductModel model) {
        return subQuery((Product p2) -> {
            SELECT(AVG(p2.getListPrice()));
            FROM(p2);
            WHERE(model == p2.getModel());
        });
    }

    @Test
    public void testCorrelatedWithExists() throws Exception {

        FluentQuery query = FluentJPA.SQL((Product p) -> {

            SELECT(DISTINCT(p.getName()));
            FROM(p);
            WHERE(EXISTS(getProductModel(p, "Long-Sleeve Logo Jersey%")));

        });

        String expected = "SELECT DISTINCT t0.name  " + "FROM Production.Product t0 " + "WHERE EXISTS (SELECT t1.* "
                + "FROM Production.Model t1 "
                + "WHERE ((t0.ProductModelID = t1.ProductModelID) AND (t1.name LIKE 'Long-Sleeve Logo Jersey%' )) )";

        assertQuery(query, expected);
    }

    private static ProductModel getProductModel(Product p,
                                         String matchCriteria) {
        return subQuery((ProductModel model) -> {
            SELECT(model);
            FROM(model);
            WHERE(p.getModel() == model && model.getName().matches(matchCriteria));
        });
    }

    @Tuple("Table1")
    @Data
    public static class Table1 {
        private int col1;
        private int col2;
        private int col3;
    }

    @Tuple("Table2")
    @Data
    public static class Table2 {
        private int col1;
        private int col2;
        private int col3;
    }

    @Test
    public void testRowCtor() throws Exception {

        FluentQuery query = FluentJPA.SQL((Table1 t1,
                                           Table2 t2) -> {

            selectAll(t1);
            WHERE(collectRows(t2, t2.getCol1(), t2.getCol2(), t2.getCol3())
                    .contains(row(t1.getCol1(), t1.getCol2(), t1.getCol3())));

        });

        String expected = "SELECT t0.* " + "FROM Table1 t0 "
                + "WHERE ((t0.col1, t0.col2, t0.col3) IN(SELECT t1.col1, t1.col2, t1.col3 " + "FROM Table2 t1 ))";

        assertQuery(query, expected);
    }

    @Test
    public void testRowCtor1() throws Exception {

        FluentQuery query = FluentJPA.SQL((Table1 t0,
                                           Table2 t1) -> {

            selectAll(t0);
            WHERE(less(pickRow(t1, t1.getCol1(), t1.getCol2(), t1.getCol3()),
                    ROW(t0.getCol1(), t0.getCol2(), t0.getCol3())));

        });

        String expected = "SELECT t0.* " + "FROM Table1 t0 " + "WHERE ((SELECT t1.col1, t1.col2, t1.col3 "
                + "FROM Table2 t1 ) < ROW(t0.col1, t0.col2, t0.col3) )";

        assertQuery(query, expected);
    }

    @Tuple("bricks")
    @Data
    public static class Brick {
        private String color;
        private int colorCount;
    }

    @Test
    public void testPickExp() throws Exception {

        FluentQuery query = FluentJPA.SQL((Brick b) -> {

            Brick countsByColor = subQuery((Brick b1) -> {
                int colorCount = alias(COUNT(), Brick::getColorCount);

                selectMany(b1, b1.getColor(), colorCount);
                GROUP(BY(b1.getColor()));
            });

            selectMany(b, b.getColor());
            GROUP(BY(b.getColor()));
            HAVING(COUNT() < pick(countsByColor, AVG(countsByColor.getColorCount())));
        });

        String expected = "SELECT t0.color " + "FROM bricks t0 " + "GROUP BY  t0.color  "
                + "HAVING (COUNT(*) < (SELECT AVG(q1.color_count) " + "FROM (SELECT t1.color, COUNT(*) AS color_count "
                + "FROM bricks t1 " + "GROUP BY  t1.color  ) q1 ))";

        assertQuery(query, expected);
    }

    @Tuple
    @Data
    @Table(name = "departments", schema = "hr")
    public static class Dept {
        private int departmentId;
    }

    @Tuple
    @Data
    @Table(name = "employees", schema = "hr")
    public static class Emp {
        private String lastName;
        @JoinColumn(name = "department_id")
        private Dept department;
    }

    @Test
    public void testUsing() throws Exception {

        FluentQuery query = FluentJPA.SQL((Dept d,
                                           Emp e) -> {
            SELECT(d.getDepartmentId(), e.getLastName());
            FROM(d).FULL_JOIN(e).USING(e1 -> e1.getDepartment().getDepartmentId());
            ORDER(BY(d.getDepartmentId()), BY(e.getLastName()));
        });

        String expected = "SELECT t0.department_id, t1.last_name "
                + "FROM hr.departments t0  FULL OUTER JOIN hr.employees t1  USING(department_id)"
                + "ORDER BY  t0.department_id ,  t1.last_name";

        assertQuery(query, expected);
    }

    public static final Sequence<Long> staticSequence = sequence("static");

    @Test
    public void testSequnce() throws Exception {

        FluentQuery query = FluentJPA.SQL(() -> {

            Sequence<Integer> sequence = sequence("myseq");
            Integer nextval = NEXT_VALUE_FOR(sequence).AS("X");

            SELECT(nextval, NEXT_VALUE_FOR(staticSequence).AS());
        });

        String expected = "SELECT NEXT VALUE FOR myseq  AS X, NEXT VALUE FOR static";

        assertQuery(query, expected);
    }

    @Tuple
    @Data
    @Table(name = "payment")
    public static class Payment {
        private int amount;
        private Timestamp paymentDate;
    }

    public static final DataType<Double> PERCENT = DataTypeNames.NUMERIC.create(10, 2);
    public static final DataType<Date> DATE = DataTypes.DATE;

    @Test
    public void testNestAggregate() throws Exception {

        FluentQuery query = FluentJPA.SQL((Payment payment) -> {

            Date paymentDate = alias(DATE.cast(payment.getPaymentDate()), "payment_date");
            int amount = alias(SUM(payment.getAmount()), "amount");

            double percentage = alias(PERCENT.cast(100 * aggregateBy(SUM(amount)).OVER(ORDER(BY(paymentDate)))
                    / aggregateBy(SUM(amount)).OVER()), "percentage");

            SELECT(paymentDate, amount, percentage);
            FROM(payment);
            GROUP(BY(paymentDate));
            ORDER(BY(paymentDate));
        });

        String expected = "SELECT CAST(t0.payment_date AS DATE) AS payment_date, SUM(t0.amount) AS amount, CAST(((100 *  SUM(SUM(t0.amount))  OVER(ORDER BY  CAST(t0.payment_date AS DATE)  )) /  SUM(SUM(t0.amount))  OVER()) AS NUMERIC(10,2)) AS percentage "
                + "FROM payment t0 " + "GROUP BY  CAST(t0.payment_date AS DATE)  "
                + "ORDER BY  CAST(t0.payment_date AS DATE)";

        assertQuery(query, expected);
    }

    @Test
    @Disabled
    public void testAssocOverride() throws Exception {

        FluentQuery query = FluentJPA.SQL((PartTimeEmployee pte) -> {

            SELECT(pte.getUser().getId());
            FROM(pte);
        });

        String expected = "SELECT NEXT VALUE FOR myseq  AS X, NEXT VALUE FOR static";

        assertQuery(query, expected);
    }
}
