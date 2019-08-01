package co.streamx.fluent.JPA.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.streamx.fluent.JPA.repository.entities.Course;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByName(String name);
}
