package co.streamx.fluent.JPA;

import static co.streamx.fluent.SQL.AggregateFunctions.COUNT;
import static co.streamx.fluent.SQL.AggregateFunctions.MAX;
import static co.streamx.fluent.SQL.Directives.alias;
import static co.streamx.fluent.SQL.Directives.aliasOf;
import static co.streamx.fluent.SQL.Directives.discardSQL;
import static co.streamx.fluent.SQL.Directives.injectSQL;
import static co.streamx.fluent.SQL.Directives.subQuery;
import static co.streamx.fluent.SQL.Directives.viewOf;
import static co.streamx.fluent.SQL.Library.COUNT;
import static co.streamx.fluent.SQL.Operators.EXISTS;
import static co.streamx.fluent.SQL.PostgreSQL.SQL.registerVendorCapabilities;
import static co.streamx.fluent.SQL.SQL.BY;
import static co.streamx.fluent.SQL.SQL.DISTINCT;
import static co.streamx.fluent.SQL.SQL.FOR;
import static co.streamx.fluent.SQL.SQL.FROM;
import static co.streamx.fluent.SQL.SQL.GROUP;
import static co.streamx.fluent.SQL.SQL.HAVING;
import static co.streamx.fluent.SQL.SQL.INSERT;
import static co.streamx.fluent.SQL.SQL.ORDER;
import static co.streamx.fluent.SQL.SQL.SELECT;
import static co.streamx.fluent.SQL.SQL.VALUES;
import static co.streamx.fluent.SQL.SQL.WHERE;
import static co.streamx.fluent.SQL.SQL.row;
import static co.streamx.fluent.SQL.ScalarFunctions.CASE;
import static co.streamx.fluent.SQL.ScalarFunctions.WHEN;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import co.streamx.fluent.JPA.ElementCollectionTypes.Address;
import co.streamx.fluent.JPA.ElementCollectionTypes.User;
import co.streamx.fluent.JPA.repository.entities.Company;
import co.streamx.fluent.JPA.repository.entities.Course;
import co.streamx.fluent.JPA.repository.entities.Employee;
import co.streamx.fluent.JPA.repository.entities.NetworkObject;
import co.streamx.fluent.JPA.repository.entities.NetworkObjectRange;
import co.streamx.fluent.JPA.repository.entities.ObjectContainer;
import co.streamx.fluent.JPA.repository.entities.Person;
import co.streamx.fluent.JPA.repository.entities.Student;
import co.streamx.fluent.SQL.ElementCollection;
import co.streamx.fluent.SQL.JoinTable;
import co.streamx.fluent.SQL.LockStrength;
import co.streamx.fluent.SQL.Versioning;
import co.streamx.fluent.SQL.TransactSQL.DataTypes;
import co.streamx.fluent.notation.Tuple;
import lombok.Data;

public class TestJPA implements CommonTest {

    @BeforeAll
    public static void init() {
        registerVendorCapabilities(FluentJPA::setCapabilities);
    }

    @Test
    public void testSelectFrom() throws Exception {
        FluentQuery builder = FluentJPA.SQL((Person p,
                                             Person p1,
                                             Person p2) -> {
            SELECT(p1.getId(), p.getId());
            FROM(p2, p, p1);
        });

        String expected = "SELECT t1.id, t0.id FROM PERSON_TABLE AS t2, PERSON_TABLE AS t0, PERSON_TABLE AS t1";
        assertQuery(builder, expected);
    }

    @Test
    public void testSelectFromLock() throws Exception {
        FluentQuery builder = FluentJPA.SQL((Person p,
                                             Person p1,
                                             Person p2) -> {
            SELECT(p1.getId(), p.getId());
            FROM(p2, p, p1);
            FOR(LockStrength.KEY_SHARE).OF(p).NOWAIT();
        });

        String expected = "SELECT t1.id, t0.id " + "FROM PERSON_TABLE AS t2, PERSON_TABLE AS t0, PERSON_TABLE AS t1 "
                + "FOR KEY SHARE  OF t0  NOWAIT";
        assertQuery(builder, expected);
    }

    @Test
    public void testSelectFromVersioning() throws Exception {
        FluentQuery builder = FluentJPA.SQL((Person p,
                                             Person p1) -> {
            SELECT(p1.getId(), p.getId());
            FROM(p, p1);
            FOR(Versioning.SYSTEM_TIME).AS_OF(DataTypes.DATE.raw("2001-10-05"));
            injectSQL("hi there!");
        });

        String expected = "SELECT t1.id, t0.id " + "FROM PERSON_TABLE AS t0, PERSON_TABLE AS t1 "
                + "FOR SYSTEM_TIME  AS OF '2001-10-05' " + "hi there!";
        assertQuery(builder, expected);
    }

    @Test
    public void testSelectFromWhere() throws Exception {
        FluentQuery builder = FluentJPA.SQL((Person p,
                                             Person p1,
                                             Person p2) -> {
            SELECT(p1.getId(), p.getId());
            FROM(p2, p, p1);
            WHERE(p.getAge() == 5);
        });

        String expected = "SELECT t1.id, t0.id FROM PERSON_TABLE AS t2, PERSON_TABLE AS t0, PERSON_TABLE AS t1 WHERE (t0.aging = 5)";
        assertQuery(builder, expected);
    }

    @Test
    public void testSelectFromAlias() throws Exception {
        FluentQuery builder = FluentJPA.SQL((Person p,
                                             Person p1,
                                             Person p2) -> {

            Person alias = alias(p, "pp");

            SELECT(p1.getId(), Math.abs((double) alias.getHeight()), Math.abs((float) p1.getAge()), alias.getId());
            FROM(p2, alias, p1);
        });

        String expected = "SELECT t1.id, ABS(pp.height), ABS(t1.aging), pp.id FROM PERSON_TABLE AS t2, PERSON_TABLE AS pp, PERSON_TABLE AS t1";
        assertQuery(builder, expected);
    }

    @Test
    public void testSelectFromAlias1() throws Exception {
        FluentQuery builder = FluentJPA.SQL((Person p,
                                             Person p1,
                                             Person p2) -> {

            Person alias = alias(p, "pp");

            SELECT(alias(alias.getAge(), Person::getHeight));
            FROM(alias);
        });

        String expected = "SELECT pp.aging AS height FROM PERSON_TABLE AS pp";
        assertQuery(builder, expected);
    }

    @Test
    public void testSelectFromSubQuery() throws Exception {
        FluentQuery builder = FluentJPA.SQL((Person p,
                                             Person p1,
                                             NetworkObjectRange s3) -> {

            Person alias = alias(p, "pp");

            Integer max = subQuery(() -> {
                SELECT(MAX(s3.getFirst()));
                FROM(s3);
            });

            SELECT(alias(alias.getAge(), Person::getHeight), alias(max, Person::getAge));
            FROM(alias);
        });

        String expected = "SELECT pp.aging AS height, (SELECT MAX(t2.first) FROM NETWORK_OBJECT_RANGE AS t2 ) AS aging FROM PERSON_TABLE AS pp";

        assertQuery(builder, expected);
    }

    @Test
    public void testSelectFromSubQueryInline() throws Exception {
        FluentQuery builder = FluentJPA.SQL((Person p,
                                             Person p1,
                                             NetworkObjectRange s3) -> {

            Person alias = alias(p, "pp");

            SELECT(alias(alias.getAge(), Person::getHeight), alias((Long) subQuery(() -> {
                SELECT(MAX(s3.getFirst()));
                FROM(s3);
            }), "aging"), subQuery(() -> {
                SELECT(MAX(s3.getFirst()));
                FROM(s3);
            }));
            FROM(alias);
        });

        String expected = "SELECT pp.aging AS height, (SELECT MAX(t2.first) "
                + "FROM NETWORK_OBJECT_RANGE AS t2 ) AS aging, (SELECT MAX(t2.first) "
                + "FROM NETWORK_OBJECT_RANGE AS t2 ) " + "FROM PERSON_TABLE AS pp";
        assertQuery(builder, expected);
    }

    @Test
    public void testSelectFromAliasSubQuery() throws Exception {
        FluentQuery builder = FluentJPA.SQL((Person p,
                                             Person p1,
                                             NetworkObjectRange s3) -> {

            Person alias = alias(p, "pp");

            NetworkObject xx = subQuery(() -> {
                SELECT(s3.getFirst());
                FROM(s3);
            });

            SELECT(alias(alias.getAge(), Person::getHeight), xx.getId(), xx.getIpCount(),
                    alias(xx.getObjectInternalType(), Person::getAge));
            FROM(alias, xx);
        });

        String expected = "SELECT pp.aging AS height, q0.id, q0.ip_count, q0.object_internal_type AS aging FROM PERSON_TABLE AS pp, (SELECT t2.first FROM NETWORK_OBJECT_RANGE AS t2 ) AS q0";
        assertQuery(builder, expected);
    }

    @Test
    public void testJoin() throws Exception {
        FluentQuery builder = FluentJPA.SQL((NetworkObject n1,
                                             ObjectContainer oc,
                                             NetworkObjectRange r) -> {

            long count = alias((long) COUNT(DISTINCT(oc.getName())), NetworkObjectRange::getLast);

            SELECT(r.getFirst(), r.getLast(), count);
            FROM(r).JOIN(n1).ON(r.getNetworkObject() == n1).JOIN(oc, n1.getObjectContainer() == oc);
            GROUP(BY(r.getFirst()), BY(r.getLast()));
            Long countAlias = aliasOf(count);
            ORDER(BY(CASE(WHEN(countAlias <= 4).THEN(countAlias)).END()).DESC().NULLS_LAST());
        });

        String expected = "SELECT t2.first, t2.last, COUNT(DISTINCT t1.name ) AS last "
                + "FROM NETWORK_OBJECT_RANGE AS t2  INNER JOIN NETWORK_OBJECT AS t0  ON (t2.NETWORK_OBJ = t0.id)  INNER JOIN OBJECT_CON AS t1 ON (t0.OBJECT_CON = t1.id) "
                + "GROUP BY  t2.first ,  t2.last  "
                + "ORDER BY  CASE WHEN (last <= 4)  THEN last   END    DESC   NULLS LAST";
        assertQuery(builder, expected);
    }

    @Test
    public void test() {

        String name = getName();

        FluentQuery query = FluentJPA.SQL((Employee p,
                                           Company c) -> {

            Company cc = subQuery(() -> {
                SELECT(c);
                FROM(p).JOIN(c).ON(p.getCompany() == c);
            });

            SELECT(p);
            FROM(p);
            WHERE((p.getName() == name || p.getName() == name || p.getName() == "Vlad Mihal'cea") && EXISTS(cc));
        });

        String expected = "SELECT t0.* FROM employee AS t0 WHERE (((t0.name = ?1) OR ((t0.name = ?1) OR (t0.name = 'Vlad Mihal''cea'))) AND EXISTS(SELECT t1.* FROM employee AS t0  INNER JOIN company AS t1  ON (t0.company_id = t1.id) ))";
        assertQuery(query, expected);
    }

    private String getName() {
        return "gggg";
    }

    @Test
    public void MTM1() throws Exception {

        String name = "c1";

        FluentQuery query = FluentJPA.SQL((Course course,
                       Student student,
                       JoinTable<Student, Course> coursesToStudents) -> {

            SELECT(COUNT(coursesToStudents.getInverseJoined().getId()));
            FROM(coursesToStudents).JOIN(course)
                    .ON(coursesToStudents.inverseJoin(course, Student::getLikedCourses))
                    .JOIN(student)
                    .ON(coursesToStudents.join(student, Student::getLikedCourses));

            WHERE(course.getName() == name);

        });

        String expected = "SELECT COUNT(t2.course_id) "
                + "FROM course_like AS t2  INNER JOIN COURSE AS t0  ON (t2.course_id = t0.id)  INNER JOIN STUDENT AS t1  ON (t2.student_id = t1.id) "
                + "WHERE (t0.name = ?1)";
        assertQuery(query, expected, new Object[] { name });
    }

    @Test
    public void MTM2() throws Exception {

        FluentQuery query = FluentJPA.SQL((Student student,
                                           JoinTable<Student, Course> coursesToStudents) -> {

            discardSQL(coursesToStudents.join(student, Student::getLikedCourses));

            INSERT().INTO(viewOf(coursesToStudents, jt -> jt.getJoined().getId(), jt -> jt.getInverseJoined().getId()));
            VALUES(row(1, 2));

        });

        String expected = "INSERT   INTO  course_like AS t1 (student_id, course_id)  " + "VALUES (1, 2)";
        assertQuery(query, expected);
    }

    @Tuple
    @Table(name = "product")
    @Data
    public class Product {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "product_id")
        private long productId;

        @ManyToMany()
        @javax.persistence.JoinTable(name = "gemstone_product", joinColumns = {
                @JoinColumn(name = "product_id") }, inverseJoinColumns = {
                @JoinColumn(name = "gemstone_id") })
        private Set<Gemstone> gemstones = new HashSet<>(0);

        // setters and getters
    }

    @Tuple
    @Table(name = "gemstone")
    @Data
    public class Gemstone {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "gemstone_id")
        private long gemstoneId;

        @ManyToMany(fetch = FetchType.LAZY)
        @javax.persistence.JoinTable(name = "gemstone_product", joinColumns = {
                @JoinColumn(name = "gemstone_id") }, inverseJoinColumns = { @JoinColumn(name = "product_id") })
        private Set<Product> products = new HashSet<>(0);

        // setters and getters
    }

    @Test
    public void MTM3() throws Exception {

        List<Long> gemstoneIds = Arrays.asList(51L, 46L);

        FluentQuery query = getProductsContainingAllStones(gemstoneIds);

        String expected = "SELECT t1.product_id " + "FROM gemstone_product AS t1 " + "WHERE (t1.gemstone_id IN ?1 ) "
                + "GROUP BY  t1.product_id  " + "HAVING (COUNT(t1.gemstone_id) = ?2)";
        assertQuery(query, expected, new Object[] { gemstoneIds, gemstoneIds.size() });
    }

    private FluentQuery getProductsContainingAllStones(List<Long> gemstoneIds) {
        int count = gemstoneIds.size();

        FluentQuery query = FluentJPA.SQL((Gemstone gemstone,
                                           JoinTable<Gemstone, Product> gemstoneProduct) -> {

            discardSQL(gemstoneProduct.join(gemstone, Gemstone::getProducts));

            long productId = gemstoneProduct.getInverseJoined().getProductId();
            long gemstoneId = gemstoneProduct.getJoined().getGemstoneId();

            SELECT(productId);
            FROM(gemstoneProduct);
            WHERE(gemstoneIds.contains(gemstoneId));
            GROUP(BY(productId));
            HAVING(COUNT(gemstoneId) == count);
        });
        return query;
    }

    @Test
    public void EC1() throws Exception {

        Long id = 1L;
        FluentQuery query = FluentJPA.SQL((User user,
                                           ElementCollection<User, String> userPhones) -> {

            discardSQL(userPhones.join(user, User::getPhoneNumbers));

            SELECT(COUNT());
            FROM(userPhones);

            WHERE(userPhones.getOwner().getId() == id);

        });

        // @formatter:off
        String expected = "SELECT COUNT(*) " + 
                "FROM EC.user_phone_numbers AS t1 " + 
                "WHERE (t1.user_id = ?1)";
        // @formatter:on
        assertQuery(query, expected, new Object[] { id });
    }

    @Test
    public void EC2() throws Exception {

        Long id = 1L;
        FluentQuery query = FluentJPA.SQL((User user,
                                           ElementCollection<User, String> userPhones) -> {

            discardSQL(userPhones.join(user, User::getPhoneNumbers));

            SELECT(userPhones.getElement());
            FROM(userPhones);

            WHERE(userPhones.getOwner().getId() == id);

        });

        // @formatter:off
        String expected = "SELECT t1.phone_number " + 
                "FROM EC.user_phone_numbers AS t1 " + 
                "WHERE (t1.user_id = ?1)";
        // @formatter:on
        assertQuery(query, expected, new Object[] { id });
    }

    @Test
    public void EC3() throws Exception {

        Long id = 1L;
        FluentQuery query = FluentJPA.SQL((User user,
                                           ElementCollection<User, Address> userAddresses) -> {

            discardSQL(userAddresses.join(user, User::getAddresses));

            SELECT(userAddresses.getElement().getAddressLine1());
            FROM(userAddresses);

            WHERE(userAddresses.getOwner().getId() == id);

        });

        // @formatter:off
        String expected = "SELECT t1.house_number " + 
                "FROM EC.user_addresses AS t1 " + 
                "WHERE (t1.user_id = ?1)";
        // @formatter:on
        assertQuery(query, expected, new Object[] { id });
    }

    @Test
    public void EC4() throws Exception {

        FluentQuery query = FluentJPA.SQL((User user,
                                           ElementCollection<User, Address> userAddresses) -> {

            discardSQL(userAddresses.join(user, User::getAddresses));

            INSERT().INTO(viewOf(userAddresses, jt -> jt.getOwner().getId(), jt -> jt.getElement().getAddressLine2()));
            VALUES(row(1, "2"));

        });

        // @formatter:off
        String expected = "INSERT   INTO  EC.user_addresses AS t1 (user_id, street)  " + 
                          "VALUES (1, '2')";
        // @formatter:on
        assertQuery(query, expected);
    }
}
