package co.streamx.fluent.JPA;

import static co.streamx.fluent.SQL.Directives.secondaryTable;
import static co.streamx.fluent.SQL.PostgreSQL.SQL.registerVendorCapabilities;
import static co.streamx.fluent.SQL.SQL.FROM;
import static co.streamx.fluent.SQL.SQL.SELECT;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class SecondaryTableTest implements CommonTest, JPAAnnotationTestTypes {
    @BeforeAll
    public static void init() {
        registerVendorCapabilities(FluentJPA::setCapabilities);
    }

    @Test
    public void testSecTable1() {

        FluentQuery query = FluentJPA.SQL((User u) -> {

            User u1 = secondaryTable(u);

            SELECT(u, u1.getName());
            FROM(u).JOIN(u1).ON(u == u1);
        });

        // @formatter:off
        String expected = "SELECT t0.*, t1.name " + 
                "FROM mtm.MEMBERS AS t0  INNER JOIN mtm.USERS AS t1  ON (t0.id = t1.UID)";
        // @formatter:on
        assertQuery(query, expected);
    }

    @Test
    public void testSecTable2() {

        FluentQuery query = FluentJPA.SQL((User u) -> {

            User u1 = secondaryTable(u, "users");

            SELECT(u, u1.getName());
            FROM(u).JOIN(u1).ON(u == u1);
        });

        // @formatter:off
        String expected = "SELECT t0.*, t1.name " + 
                "FROM mtm.MEMBERS AS t0  INNER JOIN mtm.USERS AS t1  ON (t0.id = t1.UID)";
        // @formatter:on
        assertQuery(query, expected);
    }

    @Test
    public void testSecTable3() {

        Assertions.assertThrows(IllegalStateException.class, () -> {

            FluentJPA.SQL((User u) -> {

                User u1 = secondaryTable(u, "users1");

                SELECT(u, u1.getName());
                FROM(u).JOIN(u1).ON(u == u1);
            });
        });
    }
}
