package co.streamx.fluent.JPA;

import static co.streamx.fluent.SQL.Directives.semicolon;
import static co.streamx.fluent.SQL.Directives.subQuery;
import static co.streamx.fluent.SQL.Oracle.SQL.DUAL;
import static co.streamx.fluent.SQL.Oracle.SQL.FIRST_VALUE;
import static co.streamx.fluent.SQL.Oracle.SQL.MULTISET_EXCEPT;
import static co.streamx.fluent.SQL.Oracle.SQL.MULTISET_UNION;
import static co.streamx.fluent.SQL.Oracle.SQL.ROUND;
import static co.streamx.fluent.SQL.Oracle.SQL.TO_DATE;
import static co.streamx.fluent.SQL.Oracle.SQL.TRUNC;
import static co.streamx.fluent.SQL.PostgreSQL.SQL.LIMIT;
import static co.streamx.fluent.SQL.SQL.DISTINCT;
import static co.streamx.fluent.SQL.SQL.FROM;
import static co.streamx.fluent.SQL.SQL.SELECT;
import static co.streamx.fluent.SQL.SQL.WHERE;
import static co.streamx.fluent.SQL.ScalarFunctions.CAST;
import static co.streamx.fluent.SQL.TransactSQL.SQL.HASHBYTES;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.Date;
import java.util.EnumSet;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import co.streamx.fluent.JPA.repository.entities.Person;
import co.streamx.fluent.SQL.DataType;
import co.streamx.fluent.SQL.TestSQLAggregates.Houshold;
import co.streamx.fluent.SQL.Oracle.Format;
import co.streamx.fluent.SQL.Oracle.FormatModel;
import co.streamx.fluent.SQL.Oracle.Ignore;
import co.streamx.fluent.SQL.TransactSQL.DataTypeNames;
import co.streamx.fluent.SQL.TransactSQL.DataTypes;
import co.streamx.fluent.SQL.TransactSQL.HashingAlgorithm;
import co.streamx.fluent.functions.Consumer1;
import co.streamx.fluent.functions.Function2;
import co.streamx.fluent.notation.Capability;
import co.streamx.fluent.notation.Local;

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

            SELECT(ROUND(TO_DATE("27-OCT-00"), Format.YEAR), TRUNC(TO_DATE("27-OCT-92"), DD_MON_YY));
            FROM(DUAL());
        });

        String expected = "SELECT ROUND(TO_DATE('27-OCT-00'), 'YEAR'), TRUNC(TO_DATE('27-OCT-92'), 'DD-MON-YY') "
                + "FROM DUAL";

        String sql = query.toString();
        System.out.println(sql);
        assertEquals(expected, sql.replace("\n", ""));
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

            Date constDate = DataTypes.DATE.literal("2001-10-05");
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

        String sql = query.toString();
        System.out.println(sql);
        assertEquals(expected, sql.replace("\n", ""));
    }

    @Test
    public void testExt1() {
        String name = "Dave";
        Consumer1<Person> sql = p -> {
            SELECT(p);
        };

        Consumer1<Person> sql1 = p -> {
            sql.accept(p);
            FROM(p);
        };

        Consumer1<Person> sql2 = sql1.andThen(p -> LIMIT(2));

        Function2<Person, String, Boolean> crit = (p,
                                                   n) -> {
            return dynamicWhere().apply(p, n) && p.getName() == n;
        };

        Function2<Person, String, Boolean> crit1 = dynamicWhere2(crit);

        FluentQuery query = FluentJPA.SQL((Person p) -> {
            sql2.accept(p);

            WHERE(crit1.apply(p, name));
        });

        String expected = "SELECT t0.* " + "FROM PERSON_TABLE AS t0 LIMIT 2 "
                + "WHERE (((t0.name = ?1) AND (t0.name = ?1)) AND t0.balancer)";
        assertQuery(query, expected);
    }

    @Local
    private static Function2<Person, String, Boolean> dynamicWhere() {
        return (p,
                name) -> p.getName() == name;
    }

    private static Function2<Person, String, Boolean> dynamicWhere2(Function2<Person, String, Boolean> crit) {
        return crit.and((p,
                         n) -> p.isLoadBalancer());
    }
}
