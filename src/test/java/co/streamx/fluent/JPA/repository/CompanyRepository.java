package co.streamx.fluent.JPA.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.streamx.fluent.JPA.repository.entities.Company;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {

}
