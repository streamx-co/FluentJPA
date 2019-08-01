package co.streamx.fluent.SQL;

import static co.streamx.fluent.SQL.AggregateFunctions.AVG;
import static co.streamx.fluent.SQL.AggregateFunctions.SUM;
import static co.streamx.fluent.SQL.Directives.alias;
import static co.streamx.fluent.SQL.Directives.recurseOn;
import static co.streamx.fluent.SQL.Directives.subQuery;
import static co.streamx.fluent.SQL.Directives.viewOf;
import static co.streamx.fluent.SQL.Library.COUNT;
import static co.streamx.fluent.SQL.Library.ISNULL;
import static co.streamx.fluent.SQL.Library.LIMIT;
import static co.streamx.fluent.SQL.Operators.ANY;
import static co.streamx.fluent.SQL.Operators.EXISTS;
import static co.streamx.fluent.SQL.Operators.UNION_ALL;
import static co.streamx.fluent.SQL.PostgreSQL.SQL.registerVendorCapabilities;
import static co.streamx.fluent.SQL.SQL.BY;
import static co.streamx.fluent.SQL.SQL.CUBE;
import static co.streamx.fluent.SQL.SQL.FETCH;
import static co.streamx.fluent.SQL.SQL.FROM;
import static co.streamx.fluent.SQL.SQL.GROUP;
import static co.streamx.fluent.SQL.SQL.HAVING;
import static co.streamx.fluent.SQL.SQL.OFFSET;
import static co.streamx.fluent.SQL.SQL.ORDER;
import static co.streamx.fluent.SQL.SQL.RECURSIVE;
import static co.streamx.fluent.SQL.SQL.SELECT;
import static co.streamx.fluent.SQL.SQL.WHERE;
import static co.streamx.fluent.SQL.SQL.WITH;
import static co.streamx.fluent.SQL.TransactSQL.SQL.DATEADD;
import static co.streamx.fluent.SQL.TransactSQL.SQL.YEAR;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import co.streamx.fluent.JPA.CommonTest;
import co.streamx.fluent.JPA.FluentJPA;
import co.streamx.fluent.JPA.FluentQuery;
import co.streamx.fluent.JPA.repository.entities.sqlservertutorial.Category;
import co.streamx.fluent.JPA.repository.entities.sqlservertutorial.Customer;
import co.streamx.fluent.JPA.repository.entities.sqlservertutorial.Order;
import co.streamx.fluent.JPA.repository.entities.sqlservertutorial.OrderItem;
import co.streamx.fluent.JPA.repository.entities.sqlservertutorial.Product;
import co.streamx.fluent.JPA.repository.entities.sqlservertutorial.SalesSummary;
import co.streamx.fluent.JPA.repository.entities.sqlservertutorial.Staff;
import co.streamx.fluent.JPA.repository.entities.sqlservertutorial.Store;
import co.streamx.fluent.SQL.TransactSQL.DatePart;
import co.streamx.fluent.notation.Tuple;
import lombok.Getter;

public class TestSQLServerTutorial implements CommonTest {

    @BeforeAll
    public static void init() {
        registerVendorCapabilities(FluentJPA::setCapabilities);
    }

    @Test
    public void testDateAdd() throws Exception {
        FluentQuery query = FluentJPA.SQL((Order o) -> {
            SELECT(DATEADD(DatePart.QUARTER, 2, o.getOrderDate()));
            FROM(o);
        });

        String expected = "SELECT DATEADD(QUARTER, 2, t0.order_date) " + "FROM orders AS t0";

        assertQuery(query, expected);
    }

    @Test
    public void testOffsetFetch() throws Exception {
        FluentQuery query = FluentJPA.SQL((Product p) -> {
            SELECT(p.getName(), p.getListPrice());
            FROM(p);
            ORDER(BY(p.getListPrice()), BY(p.getName()));
            OFFSET(10).ROWS();
            FETCH(10).ROWS();
        });

        String expected = "SELECT t0.product_name, t0.list_price " + "FROM products AS t0 "
                + "ORDER BY  t0.list_price ,  t0.product_name  " + "OFFSET 10  ROWS  " + "FETCH NEXT 10  ROWS ONLY";

        assertQuery(query, expected);
    }

    @Test
    public void testLeftJoin() throws Exception {
        FluentQuery query = FluentJPA.SQL((Product p,
                                           OrderItem oi) -> {
            SELECT(p.getName(), oi.getOrder());
            FROM(p).LEFT_JOIN(oi).ON(oi.getProduct() == p);
            ORDER(BY(oi.getOrder()));
        });

        String expected = "SELECT t0.product_name, t1.order_id "
                + "FROM products AS t0  LEFT JOIN order_items AS t1  ON (t1.product_id = t0.product_id) "
                + "ORDER BY  t1.order_id";

        assertQuery(query, expected);
    }

    @Test
    public void testSelfJoin() throws Exception {
        FluentQuery query = FluentJPA.SQL((Staff emp,
                                           Staff manager) -> {
            String employee = alias(emp.getFirstName().concat(" ").concat(emp.getLastName()), "employee");
            String man = alias(manager.getFirstName().concat(" ").concat(manager.getLastName()), "manager");
            SELECT(employee, man);
            FROM(emp).JOIN(manager).ON(emp.getManager() == manager);
            ORDER(BY(man));

        });

        String expected = "SELECT CONCAT(CONCAT(t0.first_name, ' '), t0.last_name) AS employee, CONCAT(CONCAT(t1.first_name, ' '), t1.last_name) AS manager "
                + "FROM staffs AS t0  INNER JOIN staffs AS t1  ON (t0.manager_id = t1.staff_id) " + "ORDER BY  manager";

        assertQuery(query, expected);
    }

    @Test
    public void testSelfJoin1() throws Exception {
        FluentQuery query = FluentJPA.SQL((Customer c1,
                                           Customer c2) -> {
            String customer_1 = alias(c1.getFirstName().concat(" ").concat(c1.getLastName()), "customer_1");
            String customer_2 = alias(c2.getFirstName() + " " + c2.getLastName(), "customer_2");

            SELECT(c1.getCity(), customer_1, customer_2);
            FROM(c1).JOIN(c2).ON(c1.getId() > c2.getId() && c1.getCity() == c2.getCity());
            ORDER(BY(c1.getCity()), BY(customer_1), BY(customer_2));

        });

        String expected = "SELECT t0.city, CONCAT(CONCAT(t0.first_name, ' '), t0.last_name) AS customer_1,  CONCAT( CONCAT(  t1.first_name  ,  ' ' ) ,  t1.last_name )  AS customer_2 "
                + "FROM customers AS t0  INNER JOIN customers AS t1  ON ((t0.customer_id > t1.customer_id) AND (t0.city = t1.city)) "
                + "ORDER BY  t0.city ,  customer_1 ,  customer_2";

        try {
            assertQuery(query, expected);
        } catch (AssertionError ae) {
            // string concatenation implementation may differ between eclipse and oracle compilers
            expected = "SELECT t0.city, CONCAT(CONCAT(t0.first_name, ' '), t0.last_name) AS customer_1, CONCAT( CONCAT( CONCAT( '' , t1.first_name ) , ' ' ) , t1.last_name ) AS customer_2 FROM customers AS t0 INNER JOIN customers AS t1 ON ((t0.customer_id > t1.customer_id) AND (t0.city = t1.city)) ORDER BY t0.city , customer_1 , customer_2";
            assertQuery(query, expected);
        }
    }

    @Tuple
    @Getter
    public static class StoreProductSales {
        private int storeId;
        private int productId;
        private Float sales;
    }

    @Test
    public void testCrossJoin() throws Exception {
        FluentQuery query = FluentJPA.SQL((Product p,
                                           Store s) -> {

            StoreProductSales storeProductSales = subQuery((Order o,
                      OrderItem i,
                      Store s1,
                      Product p1) -> {
                Float sales = alias(SUM(i.getQuantity() * i.getListPrice()), StoreProductSales::getSales);

                SELECT(alias(s.getId(), StoreProductSales::getStoreId), alias(p.getId(), StoreProductSales::getProductId),
                        sales);
                FROM(o).JOIN(i).ON(i.getOrder() == o).JOIN(s1).ON(s1 == o.getStore()).JOIN(p1).ON(p1 == i.getProduct());
                GROUP(BY(s1.getId()), BY(p1.getId()));
            });

            SELECT(s.getId(), p.getId(), alias(ISNULL(storeProductSales.getSales(), 0f), StoreProductSales::getSales));
            FROM(s).CROSS_JOIN(p)
                    .LEFT_JOIN(storeProductSales)
                    .ON(storeProductSales.getStoreId() == s.getId() && storeProductSales.getProductId() == p.getId());
            WHERE(storeProductSales.getSales() == null);
            ORDER(BY(s.getId()), BY(p.getId()));
        });

        String expected = "SELECT t1.store_id, t0.product_id, COALESCE(q0.sales, 0.0) AS sales "
                + "FROM stores AS t1  CROSS JOIN products AS t0  LEFT JOIN (SELECT t1.store_id AS store_id, t0.product_id AS product_id, SUM((t3.quantity * t3.list_price)) AS sales "
                + "FROM orders AS t2  INNER JOIN order_items AS t3  ON (t3.order_id = t2.order_id)  INNER JOIN stores AS t4  ON (t4.store_id = t2.store_id)  INNER JOIN products AS t5  ON (t5.product_id = t3.product_id) "
                + "GROUP BY  t4.store_id ,  t5.product_id  ) AS q0  ON ((q0.store_id = t1.store_id) AND (q0.product_id = t0.product_id)) "
                + "WHERE (q0.sales IS NULL) " + "ORDER BY  t1.store_id ,  t0.product_id";

        assertQuery(query, expected);
    }

    @Test
    public void testHaving() throws Exception {
        FluentQuery query = FluentJPA.SQL((OrderItem i) -> {

            Float netValue = alias(SUM(i.getQuantity() * i.getListPrice() * (1 - i.getDiscount())), "netValue");

            SELECT(i.getOrder(), netValue);
            FROM(i);
            GROUP(BY(i.getOrder()));
            HAVING(netValue > 20000);
            ORDER(BY(netValue));
        });

        String expected = "SELECT t0.order_id, SUM(((t0.quantity * t0.list_price) * (1.0 - t0.discount))) AS netValue "
                + "FROM order_items AS t0 "
                + "GROUP BY  t0.order_id  "
                + "HAVING (SUM(((t0.quantity * t0.list_price) * (1.0 - t0.discount))) > 20000.0) "
                + "ORDER BY  netValue";

        assertQuery(query, expected);
    }

    @Test
    public void testCube() throws Exception {
        FluentQuery query = FluentJPA.SQL((SalesSummary sum) -> {

            SELECT(sum.getBrand(), sum.getCategory(), alias(SUM(sum.getSales()), "sales"));
            FROM(sum);
            GROUP(CUBE(BY(sum.getBrand()), BY(sum.getCategory())));

        });

        String expected = "SELECT t0.brand, t0.category, SUM(t0.sales) AS sales " + "FROM sales_summary AS t0 "
                + "GROUP BY CUBE( t0.brand ,  t0.category )";

        assertQuery(query, expected);
    }

    @Test
    public void testSubQuery() throws Exception {
        FluentQuery query = FluentJPA.SQL((Product p) -> {
            
            double listPrice = p.getListPrice();

            SELECT(p.getName(), listPrice);
            FROM(p);

            double avgListPrice = subQuery((Product p1) -> {
                SELECT(AVG(p1.getListPrice()));
                FROM(p1);
                GROUP(BY(p1.getBrand()));
            });

            WHERE(listPrice >= ANY(avgListPrice));


        });

        String expected = "SELECT t0.product_name, t0.list_price " + "FROM products AS t0 "
                + "WHERE (t0.list_price >= ANY(SELECT AVG(t1.list_price) " + "FROM products AS t1 "
                + "GROUP BY  t1.brand_id  ))";

        assertQuery(query, expected);
    }

    @Test
    public void testSubQuery1() throws Exception {
        FluentQuery query = FluentJPA.SQL((Customer c) -> {

            SELECT(c.getId(), c.getFirstName(), c.getLastName(), c.getCity());
            FROM(c);

            List<Integer> orders2017 = subQuery((Order o) -> {
                SELECT(c.getId());
                FROM(o).JOIN(c).ON(o.getCustomer() == c);
                WHERE(YEAR(o.getOrderDate()) == 2017);
            });

            WHERE(EXISTS(orders2017));
            ORDER(BY(c.getFirstName()), BY(c.getLastName()));

        });

        String expected = "SELECT t0.customer_id, t0.first_name, t0.last_name, t0.city " + "FROM customers AS t0 "
                + "WHERE EXISTS(SELECT t0.customer_id "
                + "FROM orders AS t1  INNER JOIN customers AS t0  ON (t1.customer_id = t0.customer_id) "
                + "WHERE (YEAR(t1.order_date) = 2017) ) " + "ORDER BY  t0.first_name ,  t0.last_name";

        assertQuery(query, expected);
    }

    @Test
    public void testSubQuery2() throws Exception {
        FluentQuery query = FluentJPA.SQL((Customer c) -> {

            List<Integer> orders2017 = subQuery((Order o) -> {
                SELECT(c.getId());
                FROM(o).JOIN(c).ON(o.getCustomer() == c);
            });

            SELECT(c.getId(), c.getFirstName(), c.getLastName(), c.getCity(), orders2017.size());
            FROM(c);

            WHERE(orders2017.isEmpty());

        });

        String expected = "SELECT t0.customer_id, t0.first_name, t0.last_name, t0.city, "
                + "COUNT(*) "
                + "FROM customers AS t0 " + "WHERE NOT(EXISTS(SELECT t0.customer_id "
                + "FROM orders AS t1  INNER JOIN customers AS t0  ON (t1.customer_id = t0.customer_id) ))";

        assertQuery(query, expected);
    }

    @Tuple
    @Getter
    public static class CTESales {
        private int staffId;
        private int orderCount;
    }

    @Test
    public void testCTE_B() throws Exception {
        FluentQuery query = FluentJPA.SQL(() -> {

            CTESales cte_sales = subQuery((Order o) -> {
                SELECT(alias(o.getStaff().getId(), CTESales::getStaffId), alias(COUNT(), CTESales::getOrderCount));
                FROM(o);
                WHERE(YEAR(o.getOrderDate()) == 2018);
                GROUP(BY(o.getStaff()));
            });

            WITH(cte_sales);

            SELECT(alias(AVG(cte_sales.getOrderCount()), "average_orders_by_staff"));
            FROM(cte_sales);
        });

        String expected = "WITH q0 AS " + "(SELECT t0.staff_id AS staff_id, COUNT(*) AS order_count "
                + "FROM orders AS t0 "
                + "WHERE (YEAR(t0.order_date) = 2018) " + "GROUP BY  t0.staff_id  )"
                + "SELECT AVG(q0.order_count) AS average_orders_by_staff " + "FROM q0";

        assertQuery(query, expected);
    }

    @Tuple
    @Getter
    public static class CTESalesAmounts {
        private String staff;
        private float sales;
        private int year;
    }

    @Test
    public void testCTE_A() throws Exception {
        FluentQuery query = FluentJPA.SQL(() -> {

            CTESalesAmounts cte_sales_amounts = subQuery((Order o,
                                                          OrderItem oi,
                                                          Staff s) -> {
                String staff = alias(s.getFirstName() + " " + s.getLastName(), CTESalesAmounts::getStaff);
                Integer year = alias(YEAR(o.getOrderDate()), CTESalesAmounts::getYear);

                SELECT(staff, alias(SUM(oi.getQuantity() * oi.getListPrice() * (1 - oi.getDiscount())),
                        CTESalesAmounts::getSales), year);

                FROM(o).JOIN(oi).ON(oi.getOrder() == o).JOIN(s).ON(s == o.getStaff());

                GROUP(BY(staff), BY(year));
            });

            WITH(cte_sales_amounts);

            SELECT(cte_sales_amounts.getStaff(), cte_sales_amounts.getSales());
            FROM(cte_sales_amounts);
            WHERE(cte_sales_amounts.getYear() == 2018);
        });

        String expected = "WITH q0 AS "
                + "(SELECT  CONCAT( CONCAT(  t2.first_name  ,  ' ' ) ,  t2.last_name )  AS staff, SUM(((t1.quantity * t1.list_price) * (1.0 - t1.discount))) AS sales, YEAR(t0.order_date) AS year "
                + "FROM orders AS t0  INNER JOIN order_items AS t1  ON (t1.order_id = t0.order_id)  INNER JOIN staffs AS t2  ON (t2.staff_id = t0.staff_id) "
                + "GROUP BY   CONCAT( CONCAT(  t2.first_name  ,  ' ' ) ,  t2.last_name )  ,  YEAR(t0.order_date)  )"
                + "SELECT q0.staff, q0.sales " + "FROM q0 " + "WHERE (q0.year = 2018)";

        try {
            assertQuery(query, expected);
        } catch (AssertionError e) {
            // string concatenation implementation may differ between eclipse and oracle compilers
            expected = "WITH q0 AS (SELECT CONCAT( CONCAT( CONCAT( '' , t2.first_name ) , ' ' ) , t2.last_name ) AS staff, SUM(((t1.quantity * t1.list_price) * (1.0 - t1.discount))) AS sales, YEAR(t0.order_date) AS year FROM orders AS t0 INNER JOIN order_items AS t1 ON (t1.order_id = t0.order_id) INNER JOIN staffs AS t2 ON (t2.staff_id = t0.staff_id) GROUP BY CONCAT( CONCAT( CONCAT( '' , t2.first_name ) , ' ' ) , t2.last_name ) , YEAR(t0.order_date) )SELECT q0.staff, q0.sales FROM q0 WHERE (q0.year = 2018)";
            assertQuery(query, expected);
        }
    }

    @Tuple
    @Getter
    public static class CTECategorySales {
        private int categoryId;
        private float sales;
    }

    @Tuple
    @Getter
    public static class CTECategoryCounts {
        private int categoryId;
        private String categoryName;
        private int productCount;
    }

    @Test
    public void testCTE_C_Multiple() throws Exception {
        FluentQuery query = FluentJPA.SQL(() -> {

            CTECategoryCounts category_counts = subQuery((Product p,
                                                          Category cat) -> {

                Integer catId = alias(cat.getId(), CTECategoryCounts::getCategoryId);
                String catName = alias(cat.getName(), CTECategoryCounts::getCategoryName);
                Integer productCount = alias(COUNT(), CTECategoryCounts::getProductCount);

                SELECT(catId, catName, productCount);

                FROM(p).JOIN(cat).ON(p.getCategory() == cat);

                GROUP(BY(catId), BY(catName));
            });

            CTECategorySales category_sales = subQuery((OrderItem oi,
                                                        Product p,
                                                        Order o) -> {
                Integer catId = alias(p.getCategory().getId(), CTECategorySales::getCategoryId);
                Float sales = alias(SUM(oi.getQuantity() * oi.getListPrice() * (1 - oi.getDiscount())),
                        CTECategorySales::getSales);

                SELECT(catId, sales);

                FROM(oi).JOIN(p).ON(p == oi.getProduct()).JOIN(o).ON(o == oi.getOrder());
                WHERE(o.getStatus() == 4); // completed
                GROUP(BY(catId));
            });

            WITH(category_counts, category_sales);

            SELECT(category_counts.getCategoryId(), category_counts.getCategoryName(),
                    category_counts.getProductCount(), category_sales.getSales());

            FROM(category_counts).JOIN(category_sales)
                    .ON(category_sales.getCategoryId() == category_counts.getCategoryId());

            GROUP(BY(category_counts.getCategoryName()));
        });

        String expected = "WITH q0 AS "
                + "(SELECT t1.category_id AS category_id, t1.category_name AS category_name, COUNT(*) AS product_count "
                + "FROM products AS t0  INNER JOIN categories AS t1  ON (t0.category_id = t1.category_id) "
                + "GROUP BY  t1.category_id ,  t1.category_name  )" + ",q1 AS "
                + "(SELECT t6.category_id AS category_id, SUM(((t5.quantity * t5.list_price) * (1.0 - t5.discount))) AS sales "
                + "FROM order_items AS t5  INNER JOIN products AS t6  ON (t6.product_id = t5.product_id)  INNER JOIN orders AS t7  ON (t7.order_id = t5.order_id) "
                + "WHERE (t7.order_status = 4) "
                + "GROUP BY  t6.category_id  )" + "SELECT q0.category_id, q0.category_name, q0.product_count, q1.sales "
                + "FROM q0  INNER JOIN q1  ON (q1.category_id = q0.category_id) " + "GROUP BY  q0.category_name";

        assertQuery(query, expected);
    }

    @Tuple
    @Getter
    public static class CTENumbers {
        private int n;
    }

    @Test
    public void testCTE_Recursive_A() throws Exception {
        FluentQuery query = FluentJPA.SQL(() -> {

            CTENumbers cte_numbers = subQuery((CTENumbers t) -> {
                // initial
                SELECT(1);

                UNION_ALL();

                // recursive
                SELECT(t.getN() + 1);
                // recurse on t
                FROM(recurseOn(t));
            });

            WITH(RECURSIVE(viewOf(cte_numbers, CTENumbers::getN)));
            SELECT(cte_numbers.getN());
            FROM(cte_numbers);
            LIMIT(3);

        });

        String expected = "WITH RECURSIVE  q0 (n)   AS " + "(SELECT 1 " + "UNION ALL  " + "SELECT (t0.n + 1) "
                + "FROM q0 AS t0 )" + "SELECT q0.n " + "FROM q0 " + "FETCH NEXT 3  ROWS ONLY";

        assertQuery(query, expected);
    }

    @Entity
    @Getter
    public static class CTEOrg {
        @ManyToOne
        @JoinColumn(name = "staff_id")
        private Staff staff;

        @Column(name = "first_name")
        private String firstName;

        @ManyToOne
        @JoinColumn(name = "manager_id")
        private Staff manager;
    }

    @Test
    public void testCTE_Recursive_Org() throws Exception {
        FluentQuery query = FluentJPA.SQL(() -> {

            CTEOrg cte_org = subQuery((CTEOrg org,
                                       Staff staffManager,
                                       Staff staffSubordinate) -> {
                // initial
                SELECT(staffManager.getId(), staffManager.getFirstName(), staffManager.getManager());

                FROM(staffManager);

                WHERE(staffManager.getManager() == null);

                UNION_ALL();

                // recursive
                SELECT(staffSubordinate.getId(), staffSubordinate.getFirstName(), staffSubordinate.getManager());
                // recurse on org
                FROM(staffSubordinate).JOIN(recurseOn(org)).ON(org.getStaff() == staffSubordinate.getManager());
            });

            WITH(RECURSIVE(cte_org));
            SELECT(cte_org.getStaff(), cte_org.getFirstName(), cte_org.getManager());
            FROM(cte_org);

        });

        String expected = "WITH RECURSIVE q0  AS " + "(SELECT t1.staff_id, t1.first_name, t1.manager_id "
                + "FROM staffs AS t1 " + "WHERE (t1.manager_id IS NULL) " + "UNION ALL  "
                + "SELECT t2.staff_id, t2.first_name, t2.manager_id "
                + "FROM staffs AS t2  INNER JOIN q0 AS t0  ON (t0.staff_id = t2.staff_id) )"
                + "SELECT q0.staff_id, q0.first_name, q0.manager_id "
                + "FROM q0";

        assertQuery(query, expected);
    }
}
