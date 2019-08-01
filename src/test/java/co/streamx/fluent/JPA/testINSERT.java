package co.streamx.fluent.JPA;

import static co.streamx.fluent.SQL.Directives.noAlias;
import static co.streamx.fluent.SQL.Directives.viewOf;
import static co.streamx.fluent.SQL.Oracle.SQL.INSERT_INTO;
import static co.streamx.fluent.SQL.Oracle.SQL.LOG_ERRORS;
import static co.streamx.fluent.SQL.Oracle.SQL.REJECT_LIMIT;
import static co.streamx.fluent.SQL.Oracle.SQL.registerVendorCapabilities;
import static co.streamx.fluent.SQL.SQL.FROM;
import static co.streamx.fluent.SQL.SQL.INSERT;
import static co.streamx.fluent.SQL.SQL.SELECT;
import static co.streamx.fluent.SQL.SQL.VALUES;
import static co.streamx.fluent.SQL.SQL.WHERE;
import static co.streamx.fluent.SQL.SQL.row;
import static co.streamx.fluent.SQL.ScalarFunctions.WHEN;

import javax.persistence.Column;
import javax.persistence.Table;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import co.streamx.fluent.SQL.Oracle.InsertModifier;
import co.streamx.fluent.notation.Tuple;
import lombok.Data;
import lombok.EqualsAndHashCode;

public class testINSERT implements CommonTest {

    @BeforeAll
    public static void init() {
        registerVendorCapabilities(FluentJPA::setCapabilities);
    }

    // In real app would be an Entity
    @Tuple
    @Table(name = "orders")
    @Data
    public static class Order {
        @Column(name = "order_id")
        private int orderId;
        @Column(name = "customer_id")
        private int customerId;
        @Column(name = "order_total")
        private int orderTotal;
        @Column(name = "order_rep_id")
        private int salesRepId;
    }

    // In real app would be an Entity
    @Tuple
    @Table(name = "small_orders")
    public static class SmallOrder extends Order {
    }

    @Tuple
    @Table(name = "medium_orders")
    public static class MediumOrder extends Order {
    }

    @Tuple
    @Table(name = "large_orders")
    public static class LargeOrder extends Order {
    }

    @Tuple
    @Table(name = "special_orders")
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class SpecialOrder extends Order {
        private int special;
    }

    @Test
    public void testInsert1() throws Exception {

        int smallOrderLimit = 100000;
        int mediumOrderLimit = 200000;

        FluentQuery query = FluentJPA.SQL((Order o,
                                           Order inserted) -> {

            // the inserted objects are not necessarily Order. We insert the results of the SELECT at the bottom (kind
            // of inline sub query).
            // therefore we need a separate parameter for them (inserted).
            noAlias(inserted);

            INSERT(InsertModifier.ALL);

            WHEN(inserted.getOrderTotal() <= smallOrderLimit).THEN((SmallOrder so) -> INSERT_INTO(so));

            WHEN(inserted.getOrderTotal() > smallOrderLimit && inserted.getOrderTotal() <= mediumOrderLimit)
                    .THEN((MediumOrder mo) -> INSERT_INTO(mo));

            WHEN(inserted.getOrderTotal() > mediumOrderLimit).THEN((LargeOrder lo) -> INSERT_INTO(lo));

            SELECT(o);
            FROM(o);
        });

        String expected = "INSERT ALL " + "WHEN (order_total <= ?1)  THEN INTO small_orders  "
                + "WHEN ((order_total > ?1) AND (order_total <= ?2))  THEN INTO medium_orders  "
                + "WHEN (order_total > ?2)  THEN INTO large_orders  " + "SELECT t0.* " + "FROM orders t0";

        Object[] args = { smallOrderLimit, mediumOrderLimit };
        assertQuery(query, expected, args);
    }

    @Test
    public void testInsertElse() throws Exception {

        int smallOrderLimit = 100000;
        int mediumOrderLimit = 200000;

        FluentQuery query = FluentJPA.SQL((Order o,
                                           Order inserted) -> {

            // the inserted objects are not necessarily Order. We insert the results of the SELECT at the bottom (kind
            // of inline sub query).
            // therefore we need a separate parameter for them (inserted).
            noAlias(inserted);

            INSERT(InsertModifier.ALL);

            WHEN(inserted.getOrderTotal() <= smallOrderLimit).THEN((SmallOrder so) -> INSERT_INTO(so));

            WHEN(inserted.getOrderTotal() > smallOrderLimit && inserted.getOrderTotal() <= mediumOrderLimit)
                    .THEN((MediumOrder mo) -> INSERT_INTO(mo))
                    .ELSE((LargeOrder lo) -> INSERT_INTO(lo));

            SELECT(o);
            FROM(o);
        });

        String expected = "INSERT ALL " + "WHEN (order_total <= ?1)  THEN INTO small_orders  "
                + "WHEN ((order_total > ?1) AND (order_total <= ?2))  THEN INTO medium_orders  "
                + "ELSE INTO large_orders  " + "SELECT t0.* " + "FROM orders t0";

        Object[] args = { smallOrderLimit, mediumOrderLimit };
        assertQuery(query, expected, args);
    }

    @Test
    public void testInsertBasic() throws Exception {

        int orderTotal = 123;
        int customer = 5;

        FluentQuery query = FluentJPA.SQL((Order o) -> {

            INSERT().INTO(viewOf(o, Order::getOrderTotal, Order::getCustomerId));

            VALUES(row(orderTotal, customer));
        });

        String expected = "INSERT   INTO  orders t0 (order_total, customer_id)  " + "VALUES (?1, ?2)";

        Object[] args = { orderTotal, customer };
        assertQuery(query, expected, args);
    }

    @Tuple("raises")
    @Data
    public static class Raise {
        private int empId;
        private int sal;
    }

    @Tuple("employees")
    @Data
    public static class Employee {
        private int employeeId;
        private int salary;
        private float commissionPct;
        private int departmentId;
    }

    @Tuple("departments")
    @Data
    public static class Department {
        private int departmentId;
        private String departmentName;
    }

    @Tuple("dept_costs")
    @Data
    public static class DeptCost {
        private int deptTotal;
    }

    @Tuple("errlog")
    @Data
    public static class ErrLog {
    }

    @Test
    public void testInsertWithErrorLogging() throws Exception {

        float raise = 1.1f;
        float commissionPct = .2f;

        FluentQuery query = FluentJPA.SQL((Raise r,
                                           Employee e,
                                           ErrLog log) -> {

            INSERT().INTO(r);

            SELECT(e.getEmployeeId(), e.getSalary() * raise);
            FROM(e);
            WHERE(e.getCommissionPct() > commissionPct);

            LOG_ERRORS().INTO(log).TAG("my_bad");
            REJECT_LIMIT(10);
        });

        String expected = "INSERT   INTO raises t0 " + "SELECT t1.employee_id, (t1.salary * ?1) " + "FROM employees t1 "
                + "WHERE (t1.commission_pct > ?2) " + "LOG ERRORS   INTO errlog  ('my_bad')" + "REJECT LIMIT 10";

        Object[] args = { raise, commissionPct };
        assertQuery(query, expected, args);
    }
}
