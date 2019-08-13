package co.streamx.fluent.JPA;

import static co.streamx.fluent.SQL.AggregateFunctions.COUNT;
import static co.streamx.fluent.SQL.AggregateFunctions.MAX;
import static co.streamx.fluent.SQL.Directives.alias;
import static co.streamx.fluent.SQL.Directives.subQuery;
import static co.streamx.fluent.SQL.Operators.EXISTS;
import static co.streamx.fluent.SQL.PostgreSQL.SQL.registerVendorCapabilities;
import static co.streamx.fluent.SQL.SQL.BY;
import static co.streamx.fluent.SQL.SQL.DISTINCT;
import static co.streamx.fluent.SQL.SQL.FOR;
import static co.streamx.fluent.SQL.SQL.FROM;
import static co.streamx.fluent.SQL.SQL.GROUP;
import static co.streamx.fluent.SQL.SQL.ORDER;
import static co.streamx.fluent.SQL.SQL.SELECT;
import static co.streamx.fluent.SQL.SQL.WHERE;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import co.streamx.fluent.JPA.repository.entities.Company;
import co.streamx.fluent.JPA.repository.entities.Course;
import co.streamx.fluent.JPA.repository.entities.Employee;
import co.streamx.fluent.JPA.repository.entities.NetworkObject;
import co.streamx.fluent.JPA.repository.entities.NetworkObjectRange;
import co.streamx.fluent.JPA.repository.entities.ObjectContainer;
import co.streamx.fluent.JPA.repository.entities.Person;
import co.streamx.fluent.JPA.repository.entities.Student;
import co.streamx.fluent.SQL.JoinTable;
import co.streamx.fluent.SQL.LockStrength;

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
            SELECT(p1, p);
            FROM(p2, p, p1);
        });

        String expected = "SELECT t1.*, t0.* FROM PERSON_TABLE AS t2, PERSON_TABLE AS t0, PERSON_TABLE AS t1";
        assertQuery(builder, expected);
    }

    @Test
    public void testSelectFromLock() throws Exception {
        FluentQuery builder = FluentJPA.SQL((Person p,
                                             Person p1,
                                             Person p2) -> {
            SELECT(p1, p);
            FROM(p2, p, p1);
            FOR(LockStrength.KEY_SHARE).OF(p).NOWAIT();
        });

        String expected = "SELECT t1.*, t0.* " + "FROM PERSON_TABLE AS t2, PERSON_TABLE AS t0, PERSON_TABLE AS t1 "
                + "FOR KEY SHARE  OF t0  NOWAIT";
        assertQuery(builder, expected);
    }

    @Test
    public void testSelectFromWhere() throws Exception {
        FluentQuery builder = FluentJPA.SQL((Person p,
                                             Person p1,
                                             Person p2) -> {
            SELECT(p1, p);
            FROM(p2, p, p1);
            WHERE(p.getAge() == 5);
        });

        String expected = "SELECT t1.*, t0.* FROM PERSON_TABLE AS t2, PERSON_TABLE AS t0, PERSON_TABLE AS t1 WHERE (t0.aging = 5)";
        assertQuery(builder, expected);
    }

    @Test
    public void testSelectFromAlias() throws Exception {
        FluentQuery builder = FluentJPA.SQL((Person p,
                                             Person p1,
                                             Person p2) -> {

            Person alias = alias(p, "pp");

            SELECT(p1, Math.abs((double) alias.getHeight()), Math.abs((float) p1.getAge()), alias);
            FROM(p2, alias, p1);
        });

        String expected = "SELECT t1.*, ABS(pp.height), ABS(t1.aging), pp.* FROM PERSON_TABLE AS t2, PERSON_TABLE AS pp, PERSON_TABLE AS t1";
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

            SELECT(alias(alias.getAge(), Person::getHeight), alias((Person) subQuery(() -> {
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

            SELECT(alias(alias.getAge(), Person::getHeight), xx, xx.getIpCount(),
                    alias(xx.getObjectInternalType(), Person::getAge));
            FROM(alias, xx);
        });

        String expected = "SELECT pp.aging AS height, q0.*, q0.ip_count, q0.object_internal_type AS aging FROM PERSON_TABLE AS pp, (SELECT t2.first FROM NETWORK_OBJECT_RANGE AS t2 ) AS q0";
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
            ORDER(BY(count).DESC());
        });

        String expected = "SELECT t2.first, t2.last, COUNT(DISTINCT t1.name ) AS last "
                + "FROM NETWORK_OBJECT_RANGE AS t2  INNER JOIN NETWORK_OBJECT AS t0  ON (t2.NETWORK_OBJ = t0.id)  INNER JOIN OBJECT_CON AS t1 ON (t0.OBJECT_CON = t1.id) "
                + "GROUP BY  t2.first ,  t2.last  " + "ORDER BY  last  DESC";
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

            SELECT(COUNT(coursesToStudents.getJoined().getId()));
            FROM(course).JOIN(coursesToStudents)
                    .ON(coursesToStudents.joinBy(course.getLikes()))
                    .JOIN(student)
                    .ON(coursesToStudents.joinBy(student.getLikedCourses()));

            WHERE(course.getName() == name);

        });

        String expected = "SELECT COUNT(t2.COURSE_id) "
                + "FROM COURSE AS t0  INNER JOIN course_like AS t2  ON (t2.COURSE_id = t0.id)  INNER JOIN STUDENT AS t1  ON (t2.STUDENT_id = t1.id) "
                + "WHERE (t0.name = ?1)";
        assertQuery(query, expected, new Object[] { name });
    }
}
