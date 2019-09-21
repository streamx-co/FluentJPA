package co.streamx.fluent.JPA;

import static co.streamx.fluent.SQL.Directives.discardSQL;
import static co.streamx.fluent.SQL.Directives.typeOf;
import static co.streamx.fluent.SQL.PostgreSQL.SQL.registerVendorCapabilities;
import static co.streamx.fluent.SQL.SQL.FROM;
import static co.streamx.fluent.SQL.SQL.SELECT;
import static co.streamx.fluent.SQL.SQL.WHERE;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import co.streamx.fluent.SQL.PartialTable;

public class SecondaryTableTest implements CommonTest, JPAAnnotationTestTypes {
    @BeforeAll
    public static void init() {
        registerVendorCapabilities(FluentJPA::setCapabilities);
    }

    @Test
    public void testSecTable1() {

        FluentQuery query = FluentJPA.SQL((User u,
                                           PartialTable<User> userEx) -> {

            boolean condition = userEx.secondary(u);

            SELECT(u, u.getName());
            FROM(u).JOIN(userEx).ON(condition);
        });

        // @formatter:off
        String expected = "SELECT t0.*, t1.name " + 
                "FROM mtm.MEMBERS AS t0  INNER JOIN mtm.USERS AS t1  ON (t1.UID = t0.id)";
        // @formatter:on
        assertQuery(query, expected);
    }

    @Test
    public void testSecTable2() {

        FluentQuery query = FluentJPA.SQL((User u,
                                           PartialTable<User> userEx) -> {

            boolean condition = userEx.secondary(u, "users");

            SELECT(u, u.getName());
            FROM(u).JOIN(userEx).ON(condition);
        });

        // @formatter:off
        String expected = "SELECT t0.*, t1.name " + 
                "FROM mtm.MEMBERS AS t0  INNER JOIN mtm.USERS AS t1  ON (t1.UID = t0.id)";
        // @formatter:on
        assertQuery(query, expected);
    }

    @Test
    public void testSecTable3() {

        Assertions.assertThrows(IllegalStateException.class, () -> {

            FluentJPA.SQL((User u,
                           PartialTable<User> userEx) -> {

                boolean condition = userEx.secondary(u, "users1");

                SELECT(u, u.getName());
                FROM(u).JOIN(userEx).ON(condition);
            });
        });
    }

    @Test
    public void testTPC() {

        FluentQuery query = FluentJPA.SQL((FullTimeEmployee e) -> {

            SELECT(e.getName(), e.getSalary());
            FROM(e);
        });

        // @formatter:off
        String expected = "SELECT t0.name, t0.salary\r\n" + 
                "FROM TPC.FULL_TIME_EMP AS t0";
        // @formatter:on

        assertQuery(query, expected);
    }

    @Test
    public void testInheritJoin1() {

        FluentQuery query = FluentJPA.SQL((FullTimeEmployee1 e,
                                           PartialTable<Employee1> empEx) -> {

            boolean condition = empEx.joined(e);

            SELECT(empEx, e.getName(), e.getAge(), e.getSalary());
            FROM(e).JOIN(empEx).ON(condition);
        });

        // @formatter:off
        String expected = "SELECT t1.*, t1.name, t1.life, t0.salary\r\n" + 
                "FROM JN.FULL_TIME_EMP AS t0  INNER JOIN EMP AS t1  ON (t1.id = t0.id)";
        // @formatter:on
        assertQuery(query, expected);
    }

    @Test
    public void testInheritJoin2() {

        FluentQuery query = FluentJPA.SQL((FullTimeEmployee1 e,
                                           PartialTable<Employee1> empEx) -> {

            discardSQL(empEx.joined(e));

            SELECT(empEx);
            FROM(empEx);
        });

        // @formatter:off
        String expected = "SELECT t1.*\r\n" + 
                "FROM EMP AS t1";
        // @formatter:on
        assertQuery(query, expected);
    }

    @Test
    public void testInheritJoin3() {

        FluentQuery query = FluentJPA.SQL((Employee1 e) -> {

            SELECT(e);
            FROM(e);
            WHERE(typeOf(e, FullTimeEmployee1.class));
        });

        // @formatter:off
        String expected = "SELECT t0.*\r\n" + 
                "FROM EMP AS t0\r\n" + 
                "WHERE t0.EMP_TYPE = 'F'";
        // @formatter:on
        assertQuery(query, expected);
    }

    @Test
    public void testInheritJoin4() {

        FluentQuery query = FluentJPA.SQL((Employee1 e,
                                           FullTimeEmployee1 e1) -> {

            SELECT(e);
            FROM(e);
            WHERE(typeOf(e, e1));
        });

        // @formatter:off
        String expected = "SELECT t0.*\r\n" + 
                "FROM EMP AS t0\r\n" + 
                "WHERE t0.EMP_TYPE = 'F'";
        // @formatter:on
        assertQuery(query, expected);
    }
}
