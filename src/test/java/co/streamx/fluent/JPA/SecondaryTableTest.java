package co.streamx.fluent.JPA;

import static co.streamx.fluent.SQL.PostgreSQL.SQL.registerVendorCapabilities;
import static co.streamx.fluent.SQL.SQL.FROM;
import static co.streamx.fluent.SQL.SQL.SELECT;

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

            SELECT(empEx, e.getName(), e.getSalary());
            FROM(e).JOIN(empEx).ON(condition);
        });

        // @formatter:off
        String expected = "SELECT t1.*, t1.name, t0.salary\r\n" + 
                "FROM JN.FULL_TIME_EMP AS t0  INNER JOIN EMP AS t1  ON (t1.id = t0.id)";
        // @formatter:on
        assertQuery(query, expected);
    }
}
