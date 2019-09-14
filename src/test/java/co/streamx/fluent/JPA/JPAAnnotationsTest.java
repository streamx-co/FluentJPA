package co.streamx.fluent.JPA;

import static co.streamx.fluent.SQL.Directives.alias;
import static co.streamx.fluent.SQL.Directives.secondaryTable;
import static co.streamx.fluent.SQL.PostgreSQL.SQL.LENGTH;
import static co.streamx.fluent.SQL.PostgreSQL.SQL.registerVendorCapabilities;
import static co.streamx.fluent.SQL.SQL.FROM;
import static co.streamx.fluent.SQL.SQL.SELECT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigInteger;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import co.streamx.fluent.JPA.repository.entities.jpa.EmployeeEnumerated;
import co.streamx.fluent.SQL.DataType;
import co.streamx.fluent.SQL.PostgreSQL.DataTypeNames;
import co.streamx.fluent.notation.Tuple;
import lombok.Data;

public class JPAAnnotationsTest extends IntegrationTest implements JPAAnnotationTestTypes {

    @PersistenceContext
    private EntityManager em;

    @BeforeAll
    public static void init() {
        registerVendorCapabilities(FluentJPA::setCapabilities);
    }

    @Test
    public void testEnum() {
        FluentQuery query = FluentJPA.SQL((EmployeeEnumerated ee) -> {

            SELECT("Hello " + ee.getPayScale());
            FROM(ee);

        });

        String actual = (String) query.createQuery(em).getSingleResult();
        assertEquals("Hello MANAGER", actual);
    }

    @Test
    public void testEnumStringLength() {
        FluentQuery query = FluentJPA.SQL((EmployeeEnumerated ee) -> {

            SELECT(LENGTH(ee.getPayScale().name()));
            FROM(ee);

        });

        Number actual = (Number) query.createQuery(em).getSingleResult();
        assertEquals(7, actual.intValue());

        query = FluentJPA.SQL((EmployeeEnumerated ee) -> {

            SELECT(ee.getPayScale().name().length());
            FROM(ee);

        });

        actual = (Number) query.createQuery(em).getSingleResult();
        assertEquals(7, actual.intValue());
    }

    @Tuple
    @Data
    public static class MyTuple {

        private String hey;
        private int length;

    }

    public static DataType<String> STRING_127 = DataTypeNames.VARCHAR.create(127);

    @Test
    public void testEnumOrdinal() {
        FluentQuery query = FluentJPA.SQL((EmployeeEnumerated ee) -> {

            Integer ord = ee.getStatus().ordinal() + 3;
            SELECT(alias(STRING_127.cast(ord) + " Hey!", MyTuple::getHey),
                    alias(ee.getPayScale().name().length(), MyTuple::getLength));
            FROM(ee);

        });

        javax.persistence.Tuple actual = query.createQuery(em, javax.persistence.Tuple.class).getSingleResult();
        BigInteger integer = actual.get("length", BigInteger.class);
        assertEquals(BigInteger.valueOf(7), integer);

        MyTuple tuple = query.createQuery(em, MyTuple.class).getSingleResult();
        assertEquals("4 Hey!", tuple.getHey());
        assertEquals(7, tuple.getLength());

        FluentQuery query1 = FluentJPA.SQL((EmployeeEnumerated ee) -> {

            Integer ord = ee.getStatus().ordinal() + 3;
            SELECT(alias(STRING_127.cast(ord) + " Hey!", "ggg"),
                    alias(ee.getPayScale().name().length(), MyTuple::getLength));
            FROM(ee);

        });

        assertThrows(IndexOutOfBoundsException.class, () -> query1.createQuery(em, MyTuple.class).getSingleResult(),
                "Alias 'GGG' for column 0 not found");
    }

    @Test
    @Transactional
    public void testMTMWithDiscriminator() {
//        FluentQuery query = FluentJPA.SQL((User u) -> {
//
//            SELECT(u);
//            FROM(u);
//
//        });
//
//        query.createQuery(em, User.class).getResultList();

        User u = new User();
        String AAA = "aaa";
        u.setName(AAA);

        Group g = new Group();

        g.getMembers().add(u);
        u.getParents().add(g);

        em.persist(u);
        em.persist(g);

        em.flush();
        em.clear();

        Group g1 = em.find(Group.class, g.getId());
        assertNotNull(g1);
        assertEquals(0, g1.getParents().size());
        assertEquals(1, g1.getMembers().size());

        em.clear();

        User u1 = em.find(User.class, u.getId());
        assertNotNull(u1);
        assertEquals(1, u1.getParents().size());

        em.clear();

        FluentQuery query = FluentJPA.SQL((User uu) -> {

            User uSec = secondaryTable(uu);

            SELECT(uu, uSec.getName());
            FROM(uu).JOIN(uSec).ON(uu == uSec);
        });

        List<User> users = query.createQuery(em, User.class).getResultList();
        assertEquals(1, users.size());

        assertEquals(AAA, users.get(0).getName());
    }
}
