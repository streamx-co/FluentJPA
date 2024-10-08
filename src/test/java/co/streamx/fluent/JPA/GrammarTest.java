package co.streamx.fluent.JPA;

import static co.streamx.fluent.SQL.Directives.parameter;
import static co.streamx.fluent.SQL.Directives.semicolon;
import static co.streamx.fluent.SQL.Directives.subQuery;
import static co.streamx.fluent.SQL.Oracle.SQL.DUAL;
import static co.streamx.fluent.SQL.Oracle.SQL.FIRST_VALUE;
import static co.streamx.fluent.SQL.Oracle.SQL.MULTISET_EXCEPT;
import static co.streamx.fluent.SQL.Oracle.SQL.MULTISET_UNION;
import static co.streamx.fluent.SQL.Oracle.SQL.ROUND;
import static co.streamx.fluent.SQL.Oracle.SQL.TO_DATE;
import static co.streamx.fluent.SQL.Oracle.SQL.TRUNC;
import static co.streamx.fluent.SQL.Oracle.SQL.sequence;
import static co.streamx.fluent.SQL.PostgreSQL.SQL.LIMIT;
import static co.streamx.fluent.SQL.SQL.BY;
import static co.streamx.fluent.SQL.SQL.DISTINCT;
import static co.streamx.fluent.SQL.SQL.FROM;
import static co.streamx.fluent.SQL.SQL.ORDER;
import static co.streamx.fluent.SQL.SQL.SELECT;
import static co.streamx.fluent.SQL.SQL.WHERE;
import static co.streamx.fluent.SQL.ScalarFunctions.CAST;
import static co.streamx.fluent.SQL.TransactSQL.SQL.HASHBYTES;
import static co.streamx.fluent.SQL.TransactSQL.SQL.TOP;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import co.streamx.fluent.JPA.repository.entities.Person;
import co.streamx.fluent.SQL.DataType;
import co.streamx.fluent.SQL.TestSQLAggregates.Houshold;
import co.streamx.fluent.SQL.Oracle.Format;
import co.streamx.fluent.SQL.Oracle.FormatModel;
import co.streamx.fluent.SQL.Oracle.Ignore;
import co.streamx.fluent.SQL.Oracle.Sequence;
import co.streamx.fluent.SQL.TransactSQL.DataTypeNames;
import co.streamx.fluent.SQL.TransactSQL.DataTypes;
import co.streamx.fluent.SQL.TransactSQL.HashingAlgorithm;
import co.streamx.fluent.functions.Consumer1;
import co.streamx.fluent.functions.Function1;
import co.streamx.fluent.notation.Capability;
import co.streamx.fluent.notation.Local;
import lombok.Data;

public class GrammarTest implements CommonTest {

    @BeforeAll
    public static void init() {
        FluentJPA.setCapabilities(
                EnumSet.of(Capability.ALIAS_INSERT, Capability.ALIAS_UPDATE, Capability.TABLE_AS_ALIAS));
    }

    @Test
    public void testLiteral() throws Exception {

        FluentQuery query = FluentJPA.SQL((Houshold h) -> {

            SELECT(HASHBYTES(HashingAlgorithm.SHA2_256, "Hello!"));
        });

        String expected = "SELECT HASHBYTES('SHA2_256', 'Hello!')";

        String sql = query.toString();
        System.out.println(sql);
        assertEquals(expected, sql.replace("\n", ""));
    }

    static final FormatModel DD_MON_YY = Format.dateModel(Format.DD, Format.MON, Format.YY);

    @Test
    public void testLiteral1() throws Exception {

        FluentQuery query = FluentJPA.SQL((Houshold h) -> {

            SELECT(TOP(3).PERCENT()
                    .WITH_TIES()
                    .of(ROUND(TO_DATE("27-OCT-00"), Format.YEAR), TRUNC(TO_DATE("27-OCT-92"), DD_MON_YY)));
            FROM(DUAL());
        });

        String expected = "SELECT TOP(3) PERCENT   WITH TIES ROUND(TO_DATE('27-OCT-00'), 'YEAR'), TRUNC(TO_DATE('27-OCT-92'), 'DD-MON-YY') "
                + "FROM DUAL";

        assertQuery(query, expected);
    }

    public static final Sequence<Long> staticSequence = sequence("static");

    @Test
    public void testSequence() throws Exception {

        FluentQuery query = FluentJPA.SQL(() -> {

            Sequence<Integer> sequence = sequence("myseq");
            Integer nextval = sequence.NEXTVAL();
            SELECT(nextval, staticSequence.NEXTVAL());
            FROM(DUAL());
        });

        String expected = "SELECT myseq. NEXTVAL  , static. NEXTVAL   " + "FROM DUAL";

        assertQuery(query, expected);
    }

    @Test
    public void testMultiset() throws Exception {

        FluentQuery query = FluentJPA.SQL((Houshold h) -> {

            SELECT(MULTISET_EXCEPT(DUAL(), DISTINCT(DUAL())), MULTISET_UNION(DUAL(), DUAL()));
        });

        String expected = "SELECT  DUAL   MULTISET EXCEPT DISTINCT DUAL     ,  DUAL   MULTISET UNION DUAL";

        String sql = query.toString();
        System.out.println(sql);
        assertEquals(expected, sql.replace("\n", ""));
    }

    @Test
    public void testFirstValue() throws Exception {

        FluentQuery query = FluentJPA.SQL((Houshold h) -> {

            SELECT(FIRST_VALUE(1, Ignore.NULLS));
        });

        String expected = "SELECT FIRST_VALUE(1 IGNORE NULLS)";

        String sql = query.toString();
        System.out.println(sql);
        assertEquals(expected, sql.replace("\n", ""));
    }

    static final DataType<BigDecimal> DEC9 = DataTypeNames.DECIMAL.create(9, 0);

    @Test
    public void testDataType() throws Exception {

        FluentQuery query = FluentJPA.SQL((Houshold h) -> {

            DataType<String> VARCHAR_4000 = DataTypeNames.VARCHAR.create(4000);

            Date constDate = DataTypes.DATE.of("2001-10-05");
            SELECT(constDate);
            semicolon();
            SELECT(DataTypes.DATE.derive("{0}[]").cast("2001-10-06"));
            semicolon();
            SELECT(VARCHAR_4000.cast("2001-10-07"));
            semicolon();
            SELECT(DEC9.cast("10"));
            semicolon();
            SELECT(CAST("FFF", VARCHAR_4000));

            Houshold h1 = subQuery(() -> {
                SELECT(VARCHAR_4000.cast("2001-10-08"));
            });

            FROM(h1);

            Houshold h2 = subQuery(() -> {
                SELECT(VARCHAR_4000.cast("2001-10-09"));
            });

            FROM(h2);

            Houshold h3 = subQuery(() -> {
                SELECT(VARCHAR_4000.cast("2001-10-10"));
            });

            FROM(h3);
        });

        String expected = "SELECT DATE  '2001-10-05'  " + ";  " + "SELECT CAST('2001-10-06' AS DATE[]) " + ";  "
                + "SELECT CAST('2001-10-07' AS VARCHAR(4000)) " + ";  " + "SELECT CAST('10' AS DECIMAL(9,0)) " + ";  "
                + "SELECT CAST('FFF' AS VARCHAR(4000)) " + "FROM (SELECT CAST('2001-10-08' AS VARCHAR(4000)) ) AS q0 "
                + "FROM (SELECT CAST('2001-10-09' AS VARCHAR(4000)) ) AS q1 "
                + "FROM (SELECT CAST('2001-10-10' AS VARCHAR(4000)) ) AS q2";

        assertQuery(query, expected);
    }

    @Test
    public void testExt1() {
        List<String> names = Arrays.asList("Dave", "Steve");
        Consumer1<Person> sql = p -> {
            SELECT(p);
        };

        Consumer1<Person> sql1 = p -> {
            sql.accept(p);
            FROM(p);
        };

        Consumer1<Person> sql2 = sql1.andThen(p -> LIMIT(2));
        String x1 = names.get(0);
        String x2 = names.get(1);
        Function1<Person, Boolean> crit = (p) -> {
            return dynamicWhere(x1).apply(p) && p.getName() == parameter(x2);
        };

        Function1<Person, Boolean> crit1 = dynamicWhere2(crit);

        FluentQuery query = FluentJPA.SQL((Person p) -> {
            sql2.accept(p);

            WHERE(crit1.apply(p));
        });

        String expected = "SELECT t0.* " + "FROM PERSON_TABLE AS t0 LIMIT 2 "
                + "WHERE (((t0.name = ?1) AND (t0.name = ?2)) AND t0.balancer)";
        assertQuery(query, expected);
    }

    @Test
    public void testExt2() {
        FluentQuery query = getByNameAndAge("John", 5, false);

        String expected = "SELECT t0.* " + "FROM PERSON_TABLE AS t0 "
                + "WHERE ((t0.name = ?1) AND (t0.balancer AND (t0.balancer AND (false OR t0.balancer) ) ) ) ";
        assertQuery(query, expected);

        query = getByNameAndAge("John", 5, true);

        expected = "SELECT t0.* " + "FROM PERSON_TABLE AS t0 "
                + "WHERE ((t0.name = ?1) AND ( ( (t0.aging = ?2) OR t0.balancer) AND ( ( (t0.aging = ?3) OR t0.balancer) AND ( (t0.aging = ?4) OR t0.balancer) ) ) ) ";
        assertQuery(query, expected);
    }

    public FluentQuery getByNameAndAge(String name,
                                        int age,
                                        boolean filterByAge) {

        Function1<Person, Boolean> dynamicFilter = chain(getAgeFilter(age, filterByAge));

        Function1<Person, Boolean> dynamicFilter1 = getAgeFilter(age, filterByAge).or(Person::isLoadBalancer);

        Function1<Person, Boolean> dynamicFilter2 = p -> getAgeFilter(age, filterByAge).apply(p) || p.isLoadBalancer();

        FluentQuery query = FluentJPA.SQL((Person p) -> {
            SELECT(p);
            FROM(p);

            WHERE(p.getName() == name && dynamicFilter.apply(p) && dynamicFilter1.apply(p) && dynamicFilter2.apply(p));
        });

        return query;// .createQuery(null, Person.class).getResultList();
    }

    @Local
    private Function1<Person, Boolean> getAgeFilter(int age,
                                                    boolean filterByAge) {
        if (filterByAge)
            return (person) -> person.getAge() == parameter(age);

        return Function1.FALSE();
    }

    private static Function1<Person, Boolean> chain(Function1<Person, Boolean> filter) {
        return filter.or((p) -> p.isLoadBalancer());
    }

    @Local
    private static Function1<Person, Boolean> dynamicWhere(String name) {
        return p -> p.getName() == parameter(name);
    }

    private static Function1<Person, Boolean> dynamicWhere2(Function1<Person, Boolean> crit) {
        return crit.and((p) -> p.isLoadBalancer());
    }

    @Test
    public void testExt3() {
        String[] args = { "John%", "Dave%", "Michael%" };
        FluentQuery query = getByNameLike(Arrays.asList(args));

        String expected = "SELECT t0.* " + "FROM PERSON_TABLE AS t0 "
                + "WHERE (((t0.name LIKE ?1 ) OR (t0.name LIKE ?2 )) OR (t0.name LIKE ?3 ))";
        assertQuery(query, expected, args);
    }

    public FluentQuery getByNameLike(List<String> likes) {

        Function1<Person, Boolean> dynamicFilter = buildOr(likes);

        FluentQuery query = FluentJPA.SQL((Person p) -> {
            SELECT(p);
            FROM(p);

            WHERE(dynamicFilter.apply(p));
        });

        return query;// .createQuery(null, Person.class).getResultList();
    }

    private Function1<Person, Boolean> buildOr(List<String> likes) {
        Function1<Person, Boolean> criteria = Function1.FALSE();

        for (String like : likes)
            criteria = criteria.or(p -> p.getName().matches(parameter(like)));

        return criteria;
    }

    @Entity
    @Data // lombok
    @Table(name = "utilization_dtls", schema = "claims")
    public static class UtilizationDTL {
        @Id
        @Column(name = "utilization_dtl_id")
        private int id;

        private boolean isCompleted;
    }

    @Entity
    @Data // lombok
    @Table(name = "utilization_coverage_dtls", schema = "claims")
    public static class UtilizationCoverageDTL {
        @Id
        @Column(name = "utilization_coverage_dtl_id")
        private int id;

        @ManyToOne
        @JoinColumn(name = "utilization_dtl_id")
        private UtilizationDTL utilization;

        @ManyToOne
        @JoinColumn(name = "coverage_id")
        private CoverageMaster master;
    }

    @Entity
    @Data // lombok
    @Table(name = "coverage_master", schema = "claims")
    public static class CoverageMaster {
        @Id
        @Column(name = "coverage_id")
        private int id;

        private String coverageName;
    }

    @Test
    public void testExt4() {
        String[] args = { "John%", "Dave%", "Michael%" };
        FluentQuery query = getByNameLike1(Arrays.asList(args));

        String expected = "SELECT DISTINCT t0.utilization_dtl_id  "
                + "FROM claims.utilization_dtls AS t0  INNER JOIN claims.utilization_coverage_dtls AS t1  ON (t1.utilization_dtl_id = t0.utilization_dtl_id)  INNER JOIN claims.coverage_master AS t2  ON (t1.coverage_id = t2.coverage_id) "
                + "WHERE ((((LOWER(t2.coverage_name) LIKE ?1 ) OR (LOWER(t2.coverage_name) LIKE ?2 )) OR (LOWER(t2.coverage_name) LIKE ?3 )) AND t0.is_completed) "
                + "ORDER BY  t0.utilization_dtl_id";
        assertQuery(query, expected, args);
    }

    public FluentQuery getByNameLike1(List<String> likes) {

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
            ORDER(BY(util.getId()));
        });

        return query;// .createQuery(null, Person.class).getResultList();
    }

    private Function1<CoverageMaster, Boolean> buildOr1(List<String> likes) {
        Function1<CoverageMaster, Boolean> criteria = Function1.FALSE();

        for (String like : likes)
            criteria = criteria.or(p -> p.getCoverageName().toLowerCase().matches(parameter(like)));

        return criteria;
    }
}
