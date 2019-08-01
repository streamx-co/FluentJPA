package co.streamx.fluent.JPA.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import co.streamx.fluent.JPA.repository.entities.Phone;

@Repository
public interface PhoneRepository extends CrudRepository<Phone, String> {

}
