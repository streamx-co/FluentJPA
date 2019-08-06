package co.streamx.fluent.JPA;

import static co.streamx.fluent.SQL.AggregateFunctions.AVG;
import static co.streamx.fluent.SQL.Directives.subQuery;
import static co.streamx.fluent.SQL.Directives.viewOf;
import static co.streamx.fluent.SQL.Library.collect;
import static co.streamx.fluent.SQL.Library.pick;
import static co.streamx.fluent.SQL.Operators.BETWEEN;
import static co.streamx.fluent.SQL.Operators.EXISTS;
import static co.streamx.fluent.SQL.Operators.IN;
import static co.streamx.fluent.SQL.Operators.LIKE;
import static co.streamx.fluent.SQL.Operators.NOT;
import static co.streamx.fluent.SQL.PostgreSQL.DataTypes.DATE;
import static co.streamx.fluent.SQL.PostgreSQL.SQL.EXCLUDED;
import static co.streamx.fluent.SQL.PostgreSQL.SQL.registerVendorCapabilities;
import static co.streamx.fluent.SQL.SQL.BY;
import static co.streamx.fluent.SQL.SQL.DEFAULT;
import static co.streamx.fluent.SQL.SQL.DELETE;
import static co.streamx.fluent.SQL.SQL.DISTINCT;
import static co.streamx.fluent.SQL.SQL.FROM;
import static co.streamx.fluent.SQL.SQL.INSERT;
import static co.streamx.fluent.SQL.SQL.ON_CONFLICT;
import static co.streamx.fluent.SQL.SQL.ORDER;
import static co.streamx.fluent.SQL.SQL.SELECT;
import static co.streamx.fluent.SQL.SQL.UPDATE;
import static co.streamx.fluent.SQL.SQL.VALUES;
import static co.streamx.fluent.SQL.SQL.WHERE;
import static co.streamx.fluent.SQL.SQL.row;
import static co.streamx.fluent.SQL.ScalarFunctions.CONCAT;

import java.sql.Date;
import java.util.Arrays;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import co.streamx.fluent.JPA.repository.entities.postgresqltutorial.T1;
import co.streamx.fluent.notation.Tuple;
import lombok.Data;

public class PGTutorial implements CommonTest, PGtutorialTypes {

    @BeforeAll
    public static void init() {
        registerVendorCapabilities(FluentJPA::setCapabilities);
    }

    @Test
    public void testDISTINCT() {

        FluentQuery query = FluentJPA.SQL((T1 t1) -> {

            SELECT(DISTINCT(t1));
            FROM(t1);
            ORDER(BY(t1.getBcolor()), BY(t1.getFcolor()));
        });

        String expected = "SELECT DISTINCT t0.*  FROM T1 AS t0 ORDER BY  t0.bcolor ,  t0.fcolor";
        assertQuery(query, expected);
    }

    @Test
    public void testCONCAT() {

        FluentQuery query = FluentJPA.SQL((T1 t1) -> {

            SELECT(concat(t1.getFcolor(), t1.getBcolor()));
            FROM(t1);
        });

        String expected = "SELECT CONCAT(t0.bcolor, ' -- ', t0.fcolor) FROM T1 AS t0";
        assertQuery(query, expected);
    }

    private static String concat(String color1,
                                 String color2) {
        return CONCAT(color2, " -- ", color1);
    }

    @Test
    @Disabled
    public void testDISTINCT_ON() {

        FluentQuery query = FluentJPA.SQL((T1 t1) -> {

            // SELECT(DISTINCT_ON(T1::getBcolor, T1::getFcolor), t1);
            FROM(t1);
            ORDER(BY(t1.getBcolor()), BY(t1.getFcolor()));
        });

        String expected = "SELECT DISTINCT ON(t0.bcolor, t0.fcolor) t0.* FROM T1 AS t0 ORDER BY( t0.bcolor ,  t0.fcolor )";
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

        query = FluentJPA.SQL((T1 t1) -> {

            SELECT(t1);
            FROM(t1);
            WHERE(t1.getBcolor().matches("_Jen%"));
        });

        assertQuery(query, expected);

        query = FluentJPA.SQL((T1 t1) -> {

            SELECT(t1);
            FROM(t1);
            WHERE(NOT(LIKE(t1.getBcolor(), "_Jen%")));
        });

        expected = "SELECT t0.* FROM T1 AS t0 WHERE (NOT (t0.bcolor LIKE '_Jen%' ))";
        assertQuery(query, expected);

        query = FluentJPA.SQL((T1 t1) -> {

            SELECT(t1);
            FROM(t1);
            WHERE(!LIKE(t1.getBcolor(), "_Jen%"));
        });

        expected = "SELECT t0.* FROM T1 AS t0 WHERE NOT((t0.bcolor LIKE '_Jen%' ))";
        assertQuery(query, expected);
    }

    @Test
    public void testIS_NULL() {

        FluentQuery query = FluentJPA.SQL((T1 t1) -> {

            SELECT(t1);
            FROM(t1);
            WHERE(t1.getBcolor() == null);
        });

        String expected = "SELECT t0.* FROM T1 AS t0 WHERE (t0.bcolor IS NULL)";
        assertQuery(query, expected);

        query = FluentJPA.SQL((T1 t1) -> {

            SELECT(t1);
            FROM(t1);
            WHERE(t1.getBcolor() != null);
        });

        expected = "SELECT t0.* FROM T1 AS t0 WHERE (t0.bcolor IS NOT NULL)";
        assertQuery(query, expected);
    }

    @Test
    public void testIN() {

        List<Integer> constant = Arrays.asList(1, 2);

        FluentQuery query = FluentJPA.SQL((Rental r1) -> {

            SELECT(r1);
            FROM(r1);
            WHERE(IN(r1.getCustomer().getId(), constant));
            ORDER(BY(r1.getReturnDate()).DESC());
        });

        String expected = "SELECT t0.* FROM rental AS t0 WHERE (t0.customer_id IN ?1) ORDER BY  t0.return_date  DESC";
        assertQuery(query, expected);
    }

    @Test
    public void testNOT_IN() {

        List<Integer> constant = Arrays.asList(1, 2);

        FluentQuery query = FluentJPA.SQL((Rental r1) -> {

            SELECT(r1);
            FROM(r1);
            WHERE(NOT(IN(r1.getCustomer().getId(), constant)));
            ORDER(BY(r1.getReturnDate()).DESC());
        });

        String expected = "SELECT t0.* FROM rental AS t0 WHERE (NOT (t0.customer_id IN ?1)) ORDER BY  t0.return_date  DESC";
        assertQuery(query, expected);
    }

    @Test
    public void testBETWEEN() {

        FluentQuery query = FluentJPA.SQL((Rental r1) -> {

            SELECT(r1);
            FROM(r1);
            WHERE(BETWEEN(r1.getId(), 3, 4));
        });

        String expected = "SELECT t0.* FROM rental AS t0 WHERE (t0.rental_id BETWEEN 3 AND 4 )";
        assertQuery(query, expected);
    }

    @Test
    public void testSUBQUERY() {

        FluentQuery query = FluentJPA.SQL((Film film) -> {

            SELECT(AVG(film.getRentalRate()));
            FROM(film);

        });

        String expected = "SELECT AVG(t0.rental_rate) " + "FROM film AS t0";
        assertQuery(query, expected);

        query = FluentJPA.SQL((Film film) -> {

            pick(film, AVG(film.getRentalRate()));

        });

        assertQuery(query, expected);

        query = FluentJPA.SQL((Film film) -> {

            SELECT(film);
            FROM(film);
            WHERE(film.getRentalRate() > 2.98);
        });

        expected = "SELECT t0.* " + "FROM film AS t0 " + "WHERE (t0.rental_rate > 2.98)";
        assertQuery(query, expected);

        query = FluentJPA.SQL((Film film) -> {

            float avgRentalRate = subQuery(() -> {
                SELECT(AVG(film.getRentalRate()));
                FROM(film);
            });

            SELECT(film);
            FROM(film);
            WHERE(film.getRentalRate() > avgRentalRate);
        });

        expected = "SELECT t0.* " + "FROM film AS t0 " + "WHERE (t0.rental_rate > (SELECT AVG(t0.rental_rate) "
                + "FROM film AS t0 ))";
        assertQuery(query, expected);

        query = FluentJPA.SQL((Film film) -> {

            SELECT(film);
            FROM(film);
            WHERE(film.getRentalRate() > pick(film, AVG(film.getRentalRate())));
        });

        assertQuery(query, expected);

        query = FluentJPA.SQL((Inventory inv,
                               Rental rental) -> {

            SELECT(inv);
            FROM(inv).JOIN(rental).ON(rental.getInventory() == inv);
            WHERE(BETWEEN(rental.getReturnDate(), DATE.literal("2005-05-29"), DATE.literal("2005-05-30")));
        });

        expected = "SELECT t0.* "
                + "FROM inventory AS t0  INNER JOIN rental AS t1  ON (t1.inventory_id = t0.inventory_id) "
                + "WHERE (t1.return_date BETWEEN DATE  '2005-05-29'  AND DATE  '2005-05-30'  )";
        assertQuery(query, expected);

        Date from = new Date(2005, 4, 29); // month is zero based
        Date to = new Date(2005, 4, 30);

        query = FluentJPA.SQL(() -> {

            inventoryReturnedBetweenDates(from, to);
        });

        expected = "SELECT t0.* "
                + "FROM inventory AS t0  INNER JOIN rental AS t1  ON (t1.inventory_id = t0.inventory_id) "
                + "WHERE (t1.return_date BETWEEN ?1 AND ?2 )";
        assertQuery(query, expected);

        query = FluentJPA.SQL((Film film) -> {

            Inventory returned = inventoryReturnedBetweenDates(from, to);

            SELECT(film);
            FROM(film);
            WHERE(collect(returned, returned.getFilm().getId()).contains(film.getId()));
        });

        expected = "SELECT t0.* " + "FROM film AS t0 " + "WHERE (t0.film_id IN(SELECT q1.film_id "
                + "FROM (SELECT t1.* "
                + "FROM inventory AS t1  INNER JOIN rental AS t2  ON (t2.inventory_id = t1.inventory_id) "
                + "WHERE (t2.return_date BETWEEN ?1 AND ?2 ) ) AS q1 ))";
        assertQuery(query, expected);

        query = FluentJPA.SQL((Customer customer,
                               Payment payment) -> {

            SELECT(customer.getFirstName(), customer.getLastName());
            FROM(customer);
            WHERE(collect(payment, payment.getCustomer().getId()).contains(customer.getId()));

        });

        expected = "SELECT t0.first_name, t0.last_name " + "FROM customer AS t0 "
                + "WHERE (t0.customer_id IN(SELECT t1.customer_id " + "FROM payment AS t1 ))";
        assertQuery(query, expected);

        query = FluentJPA.SQL((Customer customer) -> {

            SELECT(customer.getFirstName(), customer.getLastName());
            FROM(customer);
            WHERE(EXISTS(customerPayments(customer)));

        });

        expected = "SELECT t0.first_name, t0.last_name " + "FROM customer AS t0 " + "WHERE EXISTS(SELECT t1.* "
                + "FROM payment AS t1 "
                + "WHERE (t1.customer_id = t0.customer_id) )";
        assertQuery(query, expected);

    }

    private static Payment customerPayments(Customer customer) {
        return subQuery((Payment payment) -> {

            SELECT(payment);
            FROM(payment);
            WHERE(payment.getCustomer() == customer);

        });
    }

    private static Inventory inventoryReturnedBetweenDates(Date from,
                                                           Date to) {

        return subQuery((Inventory inv,
                         Rental rental) -> {

            SELECT(inv);
            FROM(inv).JOIN(rental).ON(rental.getInventory() == inv);
            WHERE(BETWEEN(rental.getReturnDate(), from, to));

        });
    }

    @Test
    public void testSELF_JOIN() {

        FluentQuery query = FluentJPA.SQL((Film f1,
                                           Film f2) -> {

            SELECT(f1.getTitle(), f2.getTitle(), f1.getLength());
            FROM(f1).JOIN(f2).ON(f1.getId() != f2.getId() && f1.getLength() == f2.getLength());
        });

        String expected = "SELECT t0.title, t1.title, t0.length FROM film AS t0  INNER JOIN film AS t1  ON ((t0.film_id <> t1.film_id) AND (t0.length = t1.length))";
        assertQuery(query, expected);
    }

    @Entity(name = "departments")
    @Data
    public static class Departments {
        @Id
        @Column(name = "department_id")
        private Long id;
        @Column(name = "department_name ")
        private String name;
    }

    @Entity(name = "employees")
    @Data
    public static class Employees {
        @Id
        @Column(name = "employee_id")
        private Long id;
        @Column(name = "employee_name")
        private String name;

        @ManyToOne
        @JoinColumn(name = "department_id")
        private Departments departments;
    }

    @Test
    public void testFULL_OUTER_JOIN() {

        FluentQuery query = FluentJPA.SQL((Employees e,
                                           Departments d) -> {

            SELECT(e.getName(), d.getName());
            FROM(e).FULL_JOIN(d).ON(e.getDepartments() == d);
        });

        String expected = "SELECT t0.employee_name, t1.department_name  FROM employees AS t0  FULL OUTER JOIN departments AS t1  ON (t0.department_id = t1.department_id)";
        assertQuery(query, expected);
    }

    @Test
    public void testInsertDate() {

        FluentQuery query = FluentJPA.SQL((Link link) -> {

            INSERT().INTO(viewOf(link, Link::getUrl, Link::getName, Link::getLastUpdate));
            VALUES(row("http://www.facebook.com", "Facebook", DATE.literal("2013-06-01")));
        });

        String expected = "INSERT   INTO  link AS t0 (url, name, last_update)  "
                + "VALUES ('http://www.facebook.com', 'Facebook', DATE  '2013-06-01' )";
        assertQuery(query, expected);

        query = FluentJPA.SQL((Link link) -> {

            INSERT().INTO(viewOf(link, Link::getUrl, Link::getName, Link::getLastUpdate));
            VALUES(row("https://www.tumblr.com/", "Tumblr", DEFAULT()));
        });

        expected = "INSERT   INTO  link AS t0 (url, name, last_update)  "
                + "VALUES ('https://www.tumblr.com/', 'Tumblr', DEFAULT  )";
        assertQuery(query, expected);
    }

    @Tuple
    @Table(name = "link_tmp")
    public static class LinkTmp extends Link {
    }

    @Test
    public void testInsertFromTable() {

        FluentQuery query = FluentJPA.SQL((LinkTmp linkTmp,
                                           Link link) -> {

            INSERT().INTO(linkTmp);
            SELECT(link);
            FROM(link);
            WHERE(link.getLastUpdate() != null);
        });

        String expected = "INSERT   INTO link_tmp AS t0 " + "SELECT t1.* " + "FROM link AS t1 "
                + "WHERE (t1.last_update IS NOT NULL)";
        assertQuery(query, expected);

    }

    @Test
    public void testUpdateFromTable() {

        FluentQuery query = FluentJPA.SQL((LinkTmp linkTmp,
                                           Link link) -> {

            UPDATE(linkTmp).SET(() -> {
                linkTmp.setRel(link.getRel());
                linkTmp.setDescription(link.getDescription());
                linkTmp.setLastUpdate(link.getLastUpdate());
            });
            FROM(link);
            WHERE(linkTmp.getId() == link.getId());
        });

        String expected = "UPDATE link_tmp AS t0  SET rel = t1.rel " + "description = t1.description "
                + "last_update = t1.last_update " + "FROM link AS t1 " + "WHERE (t0.ID = t1.ID)";
        assertQuery(query, expected);

    }

    @Test
    public void testUpsert() {

        FluentQuery query = FluentJPA.SQL((Customer cust) -> {

            INSERT().INTO(viewOf(cust, Customer::getName, Customer::getEmail));
            VALUES(row("Microsoft", "hotline@microsoft.com"));
            ON_CONFLICT(Customer::getName).DO_UPDATE().SET(() -> {
                Customer excluded = EXCLUDED();
                cust.setEmail(excluded.getEmail() + ";" + cust.getEmail());
            });
        });

        String expected = "INSERT   INTO  customer AS t0 (name, email)  "
                + "VALUES ('Microsoft', 'hotline@microsoft.com') "
                + "ON CONFLICT(name) DO UPDATE   SET email =  CONCAT( CONCAT(  EXCLUDED  .email  ,  ';' ) ,  t0.email )";
        try {
            assertQuery(query, expected);
        } catch (AssertionError e) {
            expected = "INSERT INTO customer AS t0 (name, email) VALUES ('Microsoft', 'hotline@microsoft.com') ON CONFLICT (name) DO UPDATE SET email = CONCAT (CONCAT (CONCAT ('' , EXCLUDED .email) , ';') , t0.email)";
            assertQuery(query, expected);
        }

    }

    @Test
    public void testDeleteUsing() {

        FluentQuery query = FluentJPA.SQL((LinkTmp linkTmp,
                                           Link link) -> {

            DELETE().FROM(link).USING(linkTmp);
            WHERE(link.getId() == linkTmp.getId());
        });

        String expected = "DELETE   FROM link AS t1  USING link_tmp AS t0 " + "WHERE (t1.ID = t0.ID)";
        assertQuery(query, expected);

    }
}
