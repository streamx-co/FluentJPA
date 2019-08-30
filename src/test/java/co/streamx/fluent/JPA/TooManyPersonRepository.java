package co.streamx.fluent.JPA;

import static co.streamx.fluent.SQL.SQL.FROM;
import static co.streamx.fluent.SQL.SQL.SELECT;
import static co.streamx.fluent.SQL.SQL.WHERE;

import javax.persistence.NoResultException;

import org.springframework.data.jpa.repository.JpaRepository;

import co.streamx.fluent.JPA.repository.EntityManagerSupplier;

public interface TooManyPersonRepository
        extends JpaRepository<TooManyQueriesTypes.Person, Long>, TooManyQueriesTypes, EntityManagerSupplier {
    default Person findById1(Long id) {
        FluentQuery query = FluentJPA.SQL((Person person) -> {

            SELECT(person);
            FROM(person);

            WHERE(person.getId() == id);

        });

        try {
            return query.createQuery(getEntityManager(), Person.class).getSingleResult();
        } catch (NoResultException nre) {
            return null;
        }
    }
}
