package co.streamx.fluent.JPA.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.streamx.fluent.JPA.repository.entities.Student;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

}
