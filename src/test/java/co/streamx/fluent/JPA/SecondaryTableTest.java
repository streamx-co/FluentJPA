package co.streamx.fluent.JPA;

import static co.streamx.fluent.SQL.PostgreSQL.SQL.registerVendorCapabilities;
import static co.streamx.fluent.SQL.SQL.FROM;
import static co.streamx.fluent.SQL.SQL.SELECT;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import co.streamx.fluent.SQL.ExtensionTable;

public class SecondaryTableTest implements CommonTest, JPAAnnotationTestTypes {
    @BeforeAll
    public static void init() {
        registerVendorCapabilities(FluentJPA::setCapabilities);
    }

    @Test
    public void testSecTable1() {

        FluentQuery query = FluentJPA.SQL((User u,
                                           ExtensionTable<User> userEx) -> {

            boolean condition = userEx.join(u);

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
                                           ExtensionTable<User> userEx) -> {

            boolean condition = userEx.join(u, "users");

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
                           ExtensionTable<User> userEx) -> {

                boolean condition = userEx.join(u, "users1");

                SELECT(u, u.getName());
                FROM(u).JOIN(userEx).ON(condition);
            });
        });
    }
}
