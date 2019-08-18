package co.streamx.fluent.SQL;

import static co.streamx.fluent.SQL.AggregateFunctions.LAG;
import static co.streamx.fluent.SQL.AggregateFunctions.MAX;
import static co.streamx.fluent.SQL.AggregateFunctions.MIN;
import static co.streamx.fluent.SQL.AggregateFunctions.SUM;
import static co.streamx.fluent.SQL.Directives.aggregateBy;
import static co.streamx.fluent.SQL.Directives.alias;
import static co.streamx.fluent.SQL.Directives.recurseOn;
import static co.streamx.fluent.SQL.Directives.subQuery;
import static co.streamx.fluent.SQL.Directives.viewOf;
import static co.streamx.fluent.SQL.Operators.IN;
import static co.streamx.fluent.SQL.Operators.UNION_ALL;
import static co.streamx.fluent.SQL.Operators.less;
import static co.streamx.fluent.SQL.PostgreSQL.SQL.EXCLUDED;
import static co.streamx.fluent.SQL.PostgreSQL.SQL.RETURNING;
import static co.streamx.fluent.SQL.SQL.BY;
import static co.streamx.fluent.SQL.SQL.CUBE;
import static co.streamx.fluent.SQL.SQL.DEFAULT;
import static co.streamx.fluent.SQL.SQL.DEFAULT_VALUES;
import static co.streamx.fluent.SQL.SQL.DELETE;
import static co.streamx.fluent.SQL.SQL.FROM;
import static co.streamx.fluent.SQL.SQL.GROUP;
import static co.streamx.fluent.SQL.SQL.GROUPING_SETS;
import static co.streamx.fluent.SQL.SQL.HAVING;
import static co.streamx.fluent.SQL.SQL.INSERT;
import static co.streamx.fluent.SQL.SQL.ON_CONFLICT;
import static co.streamx.fluent.SQL.SQL.ORDER;
import static co.streamx.fluent.SQL.SQL.PARTITION;
import static co.streamx.fluent.SQL.SQL.RECURSIVE;
import static co.streamx.fluent.SQL.SQL.ROLLUP;
import static co.streamx.fluent.SQL.SQL.SELECT;
import static co.streamx.fluent.SQL.SQL.SET;
import static co.streamx.fluent.SQL.SQL.UPDATE;
import static co.streamx.fluent.SQL.SQL.VALUES;
import static co.streamx.fluent.SQL.SQL.WHERE;
import static co.streamx.fluent.SQL.SQL.WITH;
import static co.streamx.fluent.SQL.SQL.row;
import static co.streamx.fluent.SQL.ScalarFunctions.CASE;
import static co.streamx.fluent.SQL.ScalarFunctions.CURRENT_TIMESTAMP;
import static co.streamx.fluent.SQL.ScalarFunctions.WHEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import co.streamx.fluent.JPA.CommonTest;
import co.streamx.fluent.JPA.FluentJPA;
import co.streamx.fluent.JPA.FluentQuery;
import co.streamx.fluent.JPA.repository.entities.NetworkObject;
import co.streamx.fluent.JPA.repository.entities.NetworkObjectRange;
import co.streamx.fluent.JPA.repository.entities.ObjectContainer;
import co.streamx.fluent.JPA.repository.entities.Person;
import co.streamx.fluent.JPA.repository.entities.Sales;
import co.streamx.fluent.SQL.MySQL.Modifier;
import co.streamx.fluent.extree.expression.BlockExpression;
import co.streamx.fluent.extree.expression.Expression;
import co.streamx.fluent.extree.expression.LambdaExpression;
import co.streamx.fluent.functions.Consumer2;
import co.streamx.fluent.functions.Consumer3;
import co.streamx.fluent.functions.Function1;
import co.streamx.fluent.notation.Capability;
import co.streamx.fluent.notation.Tuple;
import lombok.Data;
import lombok.Getter;

public class TestSQL implements CommonTest {

    @BeforeAll
    public static void init() {
        FluentJPA.setCapabilities(
                EnumSet.of(Capability.ALIAS_INSERT, Capability.ALIAS_UPDATE, Capability.TABLE_AS_ALIAS));
    }

    @Test
    public void test1() throws Exception {

        Person person = new Person();
        person.setAge(56);
//        test2(person);

        Consumer2<?, ?> e = (Person p,
                             Person p1) -> {

            double tt = 8;
            MyCTE x = subQuery(() -> {

                double height = alias(p.getHeight(), "h");
                int age = alias(p.getAge() + (int) tt, "a");

                aggregateBy(MAX(p.getAge())).OVER(PARTITION(BY(p.getAge())).ORDER(BY(p.getName()).ASC()))
                        .AS(MyCTE::getAge);

                Integer age1 = aggregateBy(MAX(p.getAge()))
                        .OVER(PARTITION(BY(p.getAge())).ORDER(BY(p.getName()).ASC()))
                        .AS();
                SELECT(age, height, alias(p.getAge(), MyCTE::getHeight));
                FROM(p, p1).JOIN(p)
                        .ON(p.getHeight() == height && p.isAdult() == p1.isAdult())
                        .JOIN(p, p.getAge() == p1.getAge());
                WHERE(age > 4);
            });

            WITH(x);

            FROM(p1).JOIN(p).ON(p.getAge() == p1.getAge()).JOIN(x, p.getAge() == x.getAge());
            WHERE(p.getAge() > 4);

            SELECT(p.getAge(), p.getHeight(), "ggg", "vv");
        };

        LambdaExpression<Consumer2<?, ?>> parsed = LambdaExpression.parse(e);
        Expression body = parsed.getBody();
        assertTrue(body instanceof BlockExpression);

        List<Expression> expressions = ((BlockExpression) body).getExpressions();
        assertEquals(4, expressions.size());
    }

    interface MyCTE {
        int getAge();

        int getHeight();
    }

    void test1(Person p,
               Person p1) {
        MyCTE x = subQuery(() -> {
            SELECT(p.getAge(), p.getHeight());
            FROM(p, p1).JOIN(p).ON(p.getAge() == p1.getAge()).JOIN(p, p.getAge() == p1.getAge());
            WHERE(p.getAge() > 4);
            ORDER(BY(p.getAge()).ASC(), BY(p.getHeight()).NULLS_FIRST());
            GROUP(BY(p.getAge()));
        });

        WITH(x);

        SELECT(p.getAge(), p.getHeight(), x.getAge());
        FROM(p1).JOIN(p).ON(p.getAge() == p1.getAge()).JOIN(p, p.getAge() == p1.getAge());
        WHERE(p.getAge() > 4);
    }

    public void test2(Person p34) {

        Consumer2<?, ?> e = (Person p,
                             Person p1) -> {

            double tt = 8;
            MyCTE x = subQuery(() -> {

                double height = alias(p.getHeight(), "h");
                int age = alias(p.getAge() + (int) tt, "a");

                SELECT(age, height, alias(p.getHeight(), MyCTE::getHeight));
                FROM(p, p1).JOIN(p)
                        .ON(p.getHeight() == height && p.isAdult() == p1.isAdult())
                        .JOIN(p, p.getAge() == p1.getAge());
                WHERE(age > 4);
            });

            WITH(x);

            FROM(p1).JOIN(p).ON(p.getAge() == p34.getAge()).JOIN(x, p.getAge() == x.getAge());
            WHERE(p.getAge() > 4);

            SELECT(p.getAge(), p.getHeight(), "ggg", "vv");
        };

        LambdaExpression<Consumer2<?, ?>> parsed = LambdaExpression.parse(e);
        Expression body = parsed.getBody();
        assertTrue(body instanceof BlockExpression);

        List<Expression> expressions = ((BlockExpression) body).getExpressions();
        assertEquals(3, expressions.size());
    }

    @Test
    public void testStringConcatenation() throws Exception {
        Consumer2<?, ?> e = (Person p,
                             Person p1) -> {
            String x = p.getName() + "dgf" + this.toString() + "b";
            SELECT(x);
        };

        LambdaExpression<Consumer2<?, ?>> parsed = LambdaExpression.parse(e);
        System.out.println(parsed);
    }

    @Test
    public void test3() throws Exception {
        someMethod(null, null, null, 5, 6);
    }

    @Getter
    @Entity
    private static class NetworkObjectRangeWithParam extends NetworkObjectRange {
        private long param;
    }

    private static Long aggregateWindow(NetworkObjectRange netRange,
                                        long agg) {
        return aggregateBy(agg)
                .OVER(PARTITION(BY(netRange.getNetworkObject().getId())).ORDER(BY(netRange.getFirst()),
                        BY(netRange.getLast())))
                .AS();
    }

    public void someMethod(List<String> objectContainerNames,
                           BigInteger maxIpCount,
                           List<Integer> objectInternalTypes,
                           long minRange,
                           long maxRange) {

        Consumer3<?, ?, ?> xx = (NetworkObject net,
                                 ObjectContainer objcon,
                                 NetworkObjectRange netRange) -> {

            NetworkObjectRangeWithParam s1 = subQuery(() -> {
                long le = aggregateWindow(netRange, LAG(netRange.getLast())) + 1;

                SELECT(netRange.getNetworkObject().getId(), netRange.getFirst(), netRange.getLast(),
                        alias(le, NetworkObjectRangeWithParam::getParam));
                FROM(netRange);
            });

            NetworkObjectRangeWithParam s2 = subQuery(() -> {

                long maxLE = aggregateWindow(s1, MAX(s1.getParam()));
                Long new_start = CASE(WHEN(s1.getFirst() <= maxLE).THEN((Long) null).ELSE(s1.getFirst())).END();

                SELECT(s1.getNetworkObject().getId(), s1.getFirst(), s1.getLast(),
                        alias(new_start, NetworkObjectRangeWithParam::getParam));
                FROM(s1);
            });

            NetworkObjectRangeWithParam s3 = subQuery(() -> {

                long left_edge = aggregateWindow(s2, MAX(s2.getParam()));

                SELECT(s2.getNetworkObject().getId(), s2.getFirst(), s2.getLast(),
                        alias(left_edge, NetworkObjectRangeWithParam::getParam));
                FROM(s2);
            });

            List<Long> netObjectIds = subQuery(() -> {
                SELECT(s3.getNetworkObject());
                FROM(s3);
                GROUP(BY(s3.getNetworkObject().getId()), BY(s3.getParam()));
                HAVING(MIN(s3.getFirst()) <= minRange && MAX(s3.getLast()) >= maxRange);
            });

            SELECT(net);
            FROM(net).JOIN(objcon).ON(net.getObjectContainer() == objcon);
            WHERE(IN(objcon.getName(), objectContainerNames) && net.getIpCount().compareTo(maxIpCount) <= 0 &&
                    IN(net.getObjectInternalType(), objectInternalTypes) && netObjectIds.contains(net.getId()));
        };

        String expected = "SELECT t0.* "
                + "FROM NETWORK_OBJECT AS t0  INNER JOIN OBJECT_CON AS t1  ON (t0.OBJECT_CON = t1.id) "
                + "WHERE ((t1.name IN ?3) AND ((t0.ip_count <= ?4) AND ((t0.object_internal_type IN ?5) AND (t0.id IN(SELECT q2.NETWORK_OBJ "
                + "FROM (SELECT q1.NETWORK_OBJ, q1.first, q1.last,  MAX(q1.param)  OVER(PARTITION BY  q1.NETWORK_OBJ   ORDER BY  q1.first ,  q1.last  ) AS param "
                + "FROM (SELECT q0.NETWORK_OBJ, q0.first, q0.last, CASE WHEN (q0.first <=  MAX(q0.param)  OVER(PARTITION BY  q0.NETWORK_OBJ   ORDER BY  q0.first ,  q0.last  ))  THEN null  ELSE q0.first END   AS param "
                + "FROM (SELECT t2.NETWORK_OBJ, t2.first, t2.last, ( LAG(t2.last)  OVER(PARTITION BY  t2.NETWORK_OBJ   ORDER BY  t2.first ,  t2.last  ) + 1) AS param "
                + "FROM NETWORK_OBJECT_RANGE AS t2 ) AS q0 ) AS q1 ) AS q2 " + "GROUP BY  q2.NETWORK_OBJ ,  q2.param  "
                + "HAVING ((MIN(q2.first) <= ?1) AND (MAX(q2.last) >= ?2)) )))))";

        FluentQuery query = FluentJPA.SQL(xx);
        assertQuery(query, expected);
    }

    @Test
    public void testGroupByCase() throws Exception {
        FluentQuery query = FluentJPA.SQL((Sales s) -> {
            SELECT(s.getCountry(), s.getRegion());
            FROM(s);
            ORDER(BY(CASE(WHEN(s.getCountry() == "US").THEN(s.getSales())).END()).DESC(),
                    BY(CASE(WHEN(s.getCountry() == "CANADA").THEN(s.getSales())).END()));
        });

        String expected = "SELECT t0.country, t0.region " + "FROM Sales AS t0 "
                + "ORDER BY  CASE WHEN (t0.country = 'US')  THEN t0.sales  END    DESC  ,  CASE WHEN (t0.country = 'CANADA')  THEN t0.sales  END";

        assertQuery(query, expected);
    }

    @Test
    public void testGroupByCube() throws Exception {
        FluentQuery query = FluentJPA.SQL((Sales s) -> {
            SELECT(s.getCountry(), s.getRegion(), alias(SUM(s.getSales()), "TotalSales"));
            FROM(s);
            GROUP(BY(CUBE(s.getCountry(), s.getRegion())));
        });

        String expected = "SELECT t0.country, t0.region, SUM(t0.sales) AS TotalSales " + "FROM Sales AS t0 "
                + "GROUP BY  CUBE(t0.country, t0.region)";

        assertQuery(query, expected);
    }

    @Test
    public void testGroupByRollup() throws Exception {
        FluentQuery query = FluentJPA.SQL((Sales s) -> {
            SELECT(s.getCountry(), s.getRegion(), alias(SUM(s.getSales()), "TotalSales"));
            FROM(s);
            GROUP(BY(ROLLUP(s.getCountry(), s.getRegion())));
        });

        String expected = "SELECT t0.country, t0.region, SUM(t0.sales) AS TotalSales " + "FROM Sales AS t0 "
                + "GROUP BY  ROLLUP(t0.country, t0.region)";

        assertQuery(query, expected);
    }

    @Test
    public void testGroupByGrouping() throws Exception {
        FluentQuery query = FluentJPA.SQL((Sales s) -> {
            SELECT(s.getCountry(), s.getRegion(), alias(SUM(s.getSales()), "TotalSales"));
            FROM(s);
            GROUP(BY(GROUPING_SETS(ROLLUP(s.getCountry(), s.getRegion()), CUBE(s.getCountry(), s.getRegion()))));
        });

        String expected = "SELECT t0.country, t0.region, SUM(t0.sales) AS TotalSales " + "FROM Sales AS t0 "
                + "GROUP BY  GROUPING SETS(ROLLUP(t0.country, t0.region), CUBE(t0.country, t0.region))";

        assertQuery(query, expected);
    }

    @Test
    public void testGroupByGrouping1() throws Exception {
        FluentQuery query = FluentJPA.SQL((Sales s) -> {
            SELECT(s.getCountry(), alias(SUM(s.getSales()), "TotalSales"));
            FROM(s);
            GROUP(BY(GROUPING_SETS(SET(s.getCountry()), SET())));
        });

        String expected = "SELECT t0.country, SUM(t0.sales) AS TotalSales " + "FROM Sales AS t0 "
                + "GROUP BY  GROUPING SETS((t0.country), ())";

        assertQuery(query, expected);
    }

    @Test
    public void test4() throws Exception {
        someMethod1(null, null, null, 5, 6);
    }

    public void someMethod1(List<String> objectContainerNames,
                            BigInteger maxIpCount,
                            List<Integer> objectInternalTypes,
                            long minRange,
                            long maxRange) {

        Consumer3<?, ?, ?> xx = (NetworkObject net,
                                 ObjectContainer objcon,
                                 NetworkObjectRange netRange) -> {

            NetworkObjectRangeWithParam s1 = subQuery(() -> {
                long le = aggregateWindow(netRange, LAG(netRange.getLast())) + 1;

                SELECT(netRange.getNetworkObject().getId(), netRange.getFirst(), netRange.getLast(),
                        alias(le, NetworkObjectRangeWithParam::getParam));
                FROM(netRange);
            });

            NetworkObjectRangeWithParam s2 = subQuery(() -> {

                long maxLE = aggregateWindow(s1, MAX(s1.getParam()));
                Long new_start = CASE(WHEN(s1.getFirst() <= maxLE).THEN((Long) null).ELSE(s1.getFirst())).END();

                SELECT(s1.getNetworkObject().getId(), s1.getFirst(), s1.getLast(),
                        alias(new_start, NetworkObjectRangeWithParam::getParam));
                FROM(s1);
            });

            NetworkObjectRangeWithParam s3 = subQuery(() -> {

                long left_edge = aggregateWindow(s2, MAX(s2.getParam()));

                SELECT(s2.getNetworkObject().getId(), s2.getFirst(), s2.getLast(),
                        alias(left_edge, NetworkObjectRangeWithParam::getParam));
                FROM(s2);
            });

            List<Long> netObjectIds = subQuery(() -> {
                SELECT(s3.getNetworkObject());
                FROM(s3);
                GROUP(BY(s3.getNetworkObject().getId()), BY(s3.getParam()));
                HAVING(MIN(s3.getFirst()) <= minRange && MAX(s3.getLast()) >= maxRange);
            });

            SELECT(net);
            FROM(net).JOIN(objcon).ON(net.getObjectContainer() == objcon);
            WHERE(objectContainerNames.contains(objcon.getName()) && net.getIpCount().compareTo(maxIpCount) <= 0
                    && objectInternalTypes.contains(net.getObjectInternalType()) && netObjectIds.contains(net.getId()));
        };

        FluentQuery query = FluentJPA.SQL(xx);

        String expected = "SELECT t0.* "
                + "FROM NETWORK_OBJECT AS t0  INNER JOIN OBJECT_CON AS t1  ON (t0.OBJECT_CON = t1.id) "
                + "WHERE ((t1.name IN ?3) AND ((t0.ip_count <= ?4) AND ((t0.object_internal_type IN ?5) AND (t0.id IN(SELECT q2.NETWORK_OBJ "
                + "FROM (SELECT q1.NETWORK_OBJ, q1.first, q1.last,  MAX(q1.param)  OVER(PARTITION BY  q1.NETWORK_OBJ   ORDER BY  q1.first ,  q1.last  ) AS param "
                + "FROM (SELECT q0.NETWORK_OBJ, q0.first, q0.last, CASE WHEN (q0.first <=  MAX(q0.param)  OVER(PARTITION BY  q0.NETWORK_OBJ   ORDER BY  q0.first ,  q0.last  ))  THEN null  ELSE q0.first END   AS param "
                + "FROM (SELECT t2.NETWORK_OBJ, t2.first, t2.last, ( LAG(t2.last)  OVER(PARTITION BY  t2.NETWORK_OBJ   ORDER BY  t2.first ,  t2.last  ) + 1) AS param "
                + "FROM NETWORK_OBJECT_RANGE AS t2 ) AS q0 ) AS q1 ) AS q2 GROUP BY  q2.NETWORK_OBJ ,  q2.param  "
                + "HAVING ((MIN(q2.first) <= ?1) AND (MAX(q2.last) >= ?2)) )))))";

        assertQuery(query, expected);
    }

    static <T, C> Collection<C> mapToField(T tableRef,
                                             Function1<T, C> field) {

        return subQuery(() -> {
            SELECT(field.apply(tableRef));
            FROM(tableRef);
        });
    }

    @Getter
    @Entity
    public static class Part {
        private String name;

        @Column(name = "sub_part")
        private String subPart;
    }

    @Test
    public void testCTE_Recursive_DELETE() throws Exception {
        FluentQuery query = FluentJPA.SQL((Part allParts) -> {

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
            WHERE(mapToField(included_parts, Part::getName).contains(allParts.getName()));

        });

        String expected = "WITH RECURSIVE q1  AS " + "(SELECT t2.sub_part, t2.name " + "FROM Part AS t2 "
                + "WHERE (t2.name = 'our_product') " + "UNION ALL  " + "SELECT t3.sub_part, t3.name "
                + "FROM q1 AS t1, Part AS t3 " + "WHERE (t1.sub_part = t3.name) )" + "DELETE   " + "FROM Part "
                + "WHERE (name IN(SELECT q1.name " + "FROM q1 ))";

        assertQuery(query, expected);
    }

    @Getter
    @Tuple
    @Data
    public static class NumberLetter implements Record2<Integer, String> {
        private String letter;

        private int number;
    }

    @Test
    public void testQueriesValues() throws Exception {
        FluentQuery query = FluentJPA.SQL(() -> {
            NumberLetter numLetter = alias((NumberLetter) VALUES(row(1, "one"), row(2, "two")), "t");

            SELECT(numLetter);
            FROM(viewOf(numLetter, NumberLetter::getNumber, NumberLetter::getLetter));
        });

        String expected = "SELECT t.* FROM  (VALUES (1, 'one'), (2, 'two') ) AS t (number, letter)";

        assertQuery(query, expected);
    }

    @Test
    public void testQueriesValues1() throws Exception {
        FluentQuery query = FluentJPA.SQL(() -> {
            NumberLetter numLetter = (NumberLetter) VALUES(row(1, "one"), row(2, "two"));

            SELECT(numLetter);
            FROM(viewOf(numLetter, NumberLetter::getNumber, NumberLetter::getLetter));
        });

        String expected = "SELECT t0.* FROM  (VALUES (1, 'one'), (2, 'two') ) AS t0 (number, letter)";

        assertQuery(query, expected);
    }

    @Getter
    @Entity
    public static class Films {
        private String code;
        private String title;

        private int did;

        @Column(name = "date_prod")
        private String dateProd;
        private String kind;
        private String len;
    }

    @Test
    public void testInsert1() throws Exception {
        FluentQuery query = FluentJPA.SQL((Films f) -> {
            INSERT(Modifier.IGNORE).
            INTO(f);
            VALUES(row("UA502", "Bananas", 105, "1971-07-13", DEFAULT(), "82 minutes"));
        });

        String expected = "INSERT IGNORE  INTO Films AS t0 "
                + "VALUES ('UA502', 'Bananas', 105, '1971-07-13', DEFAULT  , '82 minutes')";

        assertQuery(query, expected);
    }

    @Test
    public void testInsertWithColumns() throws Exception {
        FluentQuery query = FluentJPA.SQL((Films f) -> {
            INSERT().INTO(viewOf(f, Films::getTitle, Films::getDid));
            VALUES(row("Bananas", 105));
        });

        String expected = "INSERT   INTO  Films AS t0 (title, did)  " + "VALUES ('Bananas', 105)";

        assertQuery(query, expected);
    }

    @Test
    public void testInsertDefaultValues() throws Exception {
        FluentQuery query = FluentJPA.SQL((Films f) -> {
            INSERT().INTO(f);
            DEFAULT_VALUES();
        });

        String expected = "INSERT   INTO Films AS t0 " + "DEFAULT VALUES";

        assertQuery(query, expected);
    }

    @Test
    public void testInsertUsingSelect() throws Exception {
        FluentQuery query = FluentJPA.SQL((Films f,
                                           Films f1) -> {
            INSERT().INTO(f);
            SELECT(f1);
            FROM(f1);
            WHERE(less(f1.getDateProd(), "2004-05-07"));
        });

        String expected = "INSERT   INTO Films AS t0 " + "SELECT t1.* " + "FROM Films AS t1 "
                + "WHERE (t1.date_prod < '2004-05-07' )";

        assertQuery(query, expected);
    }

    @Test // (expected = UnsupportedOperationException.class)
    public void testInsertUsingSelect1() throws Exception {

        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> {

            FluentQuery query = FluentJPA.SQL((Films f,
                                               Films f1) -> {
                INSERT().INTO(f);
                SELECT(f1);
                FROM(f1);
                WHERE(f1.getDateProd().equals("2004-05-07"));
            });

            String expected = "INSERT   INTO Films AS t0 " + "SELECT t1.* " + "FROM Films AS t1 "
                    + "WHERE((t1.date_prod = '2004-05-07'))";

            assertQuery(query, expected);
        });

        System.out.println(ex);
    }

    @Test
    public void testInsertReturnGeneratedId() throws Exception {
        FluentQuery query = FluentJPA.SQL((Distributor d) -> {
            INSERT().INTO(viewOf(d, Distributor::getDid, Distributor::getDname));
            VALUES(row(DEFAULT(), "XYZ Widgets"));
            RETURNING(d.getDid());
        });

        String expected = "INSERT   INTO  Distributors AS t0 (did, dname)  " + "VALUES (DEFAULT  , 'XYZ Widgets') "
                + "RETURNING t0.did";

        assertQuery(query, expected);
    }

    @Test
    public void testInsertOnConflictUpdate() throws Exception {
        FluentQuery query = FluentJPA.SQL((Distributor d) -> {

            Distributor excluded = EXCLUDED();

            INSERT().INTO(viewOf(d, Distributor::getDid, Distributor::getDname));
            VALUES(row(5, "Gizmo Transglobal"), row(6, "Associated Computing, Inc"));
            ON_CONFLICT(Distributor::getDid).DO_UPDATE().SET(() -> {
                d.setDname(excluded.getDname());
            });
        });

        String expected = "INSERT   INTO  Distributors AS t0 (did, dname)  "
                + "VALUES (5, 'Gizmo Transglobal'), (6, 'Associated Computing, Inc') "
                + "ON CONFLICT(did) DO UPDATE   SET  dname = EXCLUDED  .dname";

        assertQuery(query, expected);
    }

    @Test
    public void testInsertOnConflictDoNothing() throws Exception {
        FluentQuery query = FluentJPA.SQL((Distributor d) -> {

            INSERT().
            INTO(viewOf(d, Distributor::getDid, Distributor::getDname));
            VALUES(row(5, "Gizmo Transglobal"), row(6, "Associated Computing, Inc"));
            ON_CONFLICT(Distributor::getDid).DO_NOTHING();
        });

        String expected = "INSERT   INTO  Distributors AS t0 (did, dname)  "
                + "VALUES (5, 'Gizmo Transglobal'), (6, 'Associated Computing, Inc') "
                + "ON CONFLICT(did) DO NOTHING";

        assertQuery(query, expected);
    }

    @Data
    @Tuple
    @Table(name = "Distributors")
    public static class Distributor {
        private String dname;

        private int did;
    }

    @Data
    @Tuple
    @Table(name = "employees")
    public static class Employee {

        @Column(name = "sales_count")
        private int salesCount;

        private int id;
    }

    @Getter
    @Tuple
    @Table(name = "employees_log")
    public static class EmployeeLog {

        @Column(name = "sales_count")
        private int salesCount;

        private int id;

        @Column(name = "current_timestamp")
        private Date timestamp;
    }

    @Getter
    @Tuple
    @Table(name = "accounts")
    public static class Account {

        @Column(name = "sales_person")
        private int salesPerson;

        private String name;
    }

    @Test
    public void testInsertUpdate() throws Exception {
        FluentQuery query = FluentJPA.SQL((EmployeeLog log) -> {

            Employee upd = subQuery((Employee emp) -> {

                int salesPerson = subQuery((Account account) -> {
                    SELECT(account.getSalesPerson());
                    FROM(account);
                    WHERE(account.getName() == "Acme Corporation");
                });

                // UPDATE(emp).SET(assign(Employee::getSalesCount, emp.getSalesCount() + 1));
                UPDATE(emp).SET(() -> {
                    emp.setSalesCount(emp.getSalesCount() + 1);
                });
                WHERE(emp.getId() == salesPerson);
                RETURNING(emp);

            });

            WITH(upd);

            INSERT().
            INTO(log);
            SELECT(upd.getId(), CURRENT_TIMESTAMP());
            FROM(upd);
        });

        String expected = "WITH q0 AS " + "(UPDATE employees AS t1  SET  sales_count = (t1.sales_count + 1)   "
                + "WHERE (t1.id = (SELECT t2.sales_person " + "FROM accounts AS t2 "
                + "WHERE (t2.name = 'Acme Corporation') )) " + "RETURNING t1.* )" + "INSERT   INTO employees_log AS t0 "
                + "SELECT q0.id, CURRENT_TIMESTAMP   " + "FROM q0";

        assertQuery(query, expected);
    }
}
