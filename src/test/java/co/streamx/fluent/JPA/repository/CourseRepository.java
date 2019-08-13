package co.streamx.fluent.JPA.repository;

import static co.streamx.fluent.SQL.AggregateFunctions.COUNT;
import static co.streamx.fluent.SQL.SQL.FROM;
import static co.streamx.fluent.SQL.SQL.SELECT;
import static co.streamx.fluent.SQL.SQL.WHERE;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.streamx.fluent.JPA.FluentJPA;
import co.streamx.fluent.JPA.FluentQuery;
import co.streamx.fluent.JPA.repository.entities.Course;
import co.streamx.fluent.JPA.repository.entities.Student;
import co.streamx.fluent.SQL.JoinTable;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long>, EntityManagerSupplier {
    List<Course> findByName(String name);

    default int countStudents(String name) {
        FluentQuery query = FluentJPA.SQL((Course course,
                                           JoinTable<Student, Course> coursesToStudents) -> {

            SELECT(COUNT(coursesToStudents.getJoined().getId()));
            FROM(course, coursesToStudents);

            WHERE(coursesToStudents.joinBy(course.getLikes()) && course.getName() == name);

        });

        Number number = (Number) query.createQuery(getEntityManager()).getSingleResult();
        return number.intValue();
    }

    default int countStudents1(String name) {
        FluentQuery query = FluentJPA.SQL((Course course,
                                           Student student,
                                           JoinTable<Student, Course> coursesToStudents) -> {

            SELECT(COUNT(student.getId()));
            FROM(course).JOIN(coursesToStudents)
                    .ON(coursesToStudents.joinBy(course.getLikes()))
                    .JOIN(student)
                    .ON(coursesToStudents.joinBy(student.getLikedCourses()));

            WHERE(course.getName() == name);

        });

        Number number = (Number) query.createQuery(getEntityManager()).getSingleResult();
        return number.intValue();
    }
}
