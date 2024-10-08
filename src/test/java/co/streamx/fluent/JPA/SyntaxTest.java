package co.streamx.fluent.JPA;

import static co.streamx.fluent.SQL.Operators.BETWEEN;
import static co.streamx.fluent.SQL.Operators.LIKE;
import static co.streamx.fluent.SQL.Operators.NOT;
import static co.streamx.fluent.SQL.PostgreSQL.SQL.registerVendorCapabilities;
import static co.streamx.fluent.SQL.SQL.FROM;
import static co.streamx.fluent.SQL.SQL.SELECT;
import static co.streamx.fluent.SQL.SQL.WHERE;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import co.streamx.fluent.JPA.repository.entities.postgresqltutorial.T1;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

public class SyntaxTest extends IntegrationTest {

    @Entity
    @Data
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    @NoArgsConstructor
    public static class Rental {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(name = "customer_id")
        private long customerId;

        @Column(name = "return_date")
        private Date returnDate;

    }

    @PersistenceContext
    private EntityManager em;

    @BeforeAll
    public static void init() {
        registerVendorCapabilities(FluentJPA::setCapabilities);
    }

    @Test
    public void testDISTINCT_ON() {

        Query nq = em.createNativeQuery(
                "SELECT DISTINCT ON(t0.bcolor, t0.fcolor) t0.* FROM T1 t0 ORDER BY( t0.bcolor ,  t0.fcolor )");
        nq.getResultList();

//        FluentQuery query = FluentJPA.SQL((T1 t1) -> {
//
//            SELECT(DISTINCT_ON(T1::getBcolor, T1::getFcolor), t1);
//            FROM(t1);
//            ORDER(BY(t1.getBcolor()), BY(t1.getFcolor()));
//        });
//
//        String sql = query.toString();
//        System.out.println(sql);
//
//        TypedQuery<Company> q = query.createQuery(em, Company.class);
//        q.getResultList();
//        assertEquals("SELECT DISTINCT t0  FROM T1 t0 ORDER BY( t0.bcolor ,  t0.fcolor )", sql.replace("\n", ""));
    }

    @Test
    public void testNOT_IN() {

        List<Integer> constant = Arrays.asList(1, 2);
        Query nq = em.createNativeQuery(
                "SELECT t0.* FROM Rental t0 WHERE ((t0.customer_id NOT IN (?1))) ORDER BY (t0.return_date) DESC, (t0.customer_id) ASC")
                .setParameter(1, constant);
        nq.getResultList();
    }

    @Test
    public void testNOT_IN1() {

        List<Integer> constant = Arrays.asList(1, 2);
        Query nq = em.createNativeQuery(
                "SELECT t0.* FROM Rental t0 WHERE (NOT(NOT(t0.customer_id IN (?1)))) ORDER BY (t0.return_date) DESC, (t0.customer_id) ASC")
                .setParameter(1, constant);
        nq.getResultList();
    }

    @Test
    public void testIS_NULL() {

        Query nq = em.createNativeQuery(
                "SELECT t0.* FROM Rental t0 WHERE (NOT((t0.customer_id IS NOT NULL))) ORDER BY (t0.return_date) DESC, (t0.customer_id) ASC");
        nq.getResultList();
    }

    @Test
    public void testBETWEEN() {

        FluentQuery query = FluentJPA.SQL((Rental r1) -> {

            SELECT(r1);
            FROM(r1);
            WHERE(BETWEEN(r1.getId(), 3, 4));
        });

        query.createQuery(em, Rental.class).getResultList();

        String expected = "SELECT t0.* FROM Rental AS t0 WHERE (t0.id BETWEEN 3 AND 4 )";
        assertQuery(query, expected);
    }

    @Test
    public void testLIKE() {

        FluentQuery query = FluentJPA.SQL((T1 t1) -> {

            SELECT(t1);
            FROM(t1);
            WHERE(LIKE(t1.getBcolor(), "_Jen%"));
        });

        String expected = "SELECT t0.* FROM T1 AS t0 WHERE (t0.bcolor LIKE '_Jen%' )";
        assertQuery(query, expected);
        query.createQuery(em, T1.class).getResultList();

        query = FluentJPA.SQL((T1 t1) -> {

            SELECT(t1);
            FROM(t1);
            WHERE(NOT(LIKE(t1.getBcolor(), "_Jen%")));
        });

        expected = "SELECT t0.* FROM T1 AS t0 WHERE (NOT (t0.bcolor LIKE '_Jen%' ))";
        assertQuery(query, expected);
        query.createQuery(em, T1.class).getResultList();

        query = FluentJPA.SQL((T1 t1) -> {

            SELECT(t1);
            FROM(t1);
            WHERE(!LIKE(t1.getBcolor(), "_Jen%"));
        });

        expected = "SELECT t0.* FROM T1 AS t0 WHERE NOT((t0.bcolor LIKE '_Jen%' ))";
        assertQuery(query, expected);
        query.createQuery(em, T1.class).getResultList();
    }
}
