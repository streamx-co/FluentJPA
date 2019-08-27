package co.streamx.fluent.JPA.repository;

import static co.streamx.fluent.SQL.AggregateFunctions.COUNT;
import static co.streamx.fluent.SQL.SQL.FROM;
import static co.streamx.fluent.SQL.SQL.SELECT;
import static co.streamx.fluent.SQL.SQL.WHERE;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.streamx.fluent.JPA.CommonTest;
import co.streamx.fluent.JPA.FluentJPA;
import co.streamx.fluent.JPA.FluentQuery;
import co.streamx.fluent.JPA.repository.entities.Course;
import co.streamx.fluent.JPA.repository.entities.Student;
import co.streamx.fluent.SQL.JoinTable;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long>, EntityManagerSupplier, CommonTest {
    List<Course> findByName(String name);

    default int countStudents(String name) {
        FluentQuery query = FluentJPA.SQL((Course course,
                                           JoinTable<Student, Course> coursesToStudents) -> {

            SELECT(COUNT(coursesToStudents.getJoined().getId()));
            FROM(course, coursesToStudents);

            WHERE(coursesToStudents.join(course, Course::getLikes) && course.getName() == name);

        });

        // @formatter:off
        String expected = "SELECT COUNT(t1.student_id) " + 
                "FROM COURSE t0, course_like t1 " + 
                "WHERE ((t1.course_id = t0.id) AND (t0.name = ?1))";
        // @formatter:on

        assertQuery(query, expected);

        Number number = (Number) query.createQuery(getEntityManager()).getSingleResult();
        return number.intValue();
    }

    default int countStudents1(String name) {
        FluentQuery query = FluentJPA.SQL((Course course,
                                           Student student,
                                           JoinTable<Student, Course> coursesToStudents) -> {

            SELECT(COUNT(student.getId()));
            FROM(course).JOIN(coursesToStudents)
                    .ON(coursesToStudents.join(course, Course::getLikes))
                    .JOIN(student)
                    .ON(coursesToStudents.join(student, Student::getLikedCourses));

            WHERE(course.getName() == name);

        });

        // @formatter:off
        String expected = "SELECT COUNT(t1.id) " + 
                "FROM COURSE t0  INNER JOIN course_like t2  ON (t2.course_id = t0.id)  INNER JOIN STUDENT t1  ON (t2.student_id = t1.id) " + 
                "WHERE (t0.name = ?1)";
        // @formatter:on

        assertQuery(query, expected, arrayOf(name));

        Number number = (Number) query.createQuery(getEntityManager()).getSingleResult();
        return number.intValue();
    }
}
