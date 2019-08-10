package co.streamx.fluent.JPA.repository;

import static co.streamx.fluent.SQL.Directives.viewOf;
import static co.streamx.fluent.SQL.MySQL.SQL.LAST_INSERT_ID;
import static co.streamx.fluent.SQL.SQL.FROM;
import static co.streamx.fluent.SQL.SQL.INSERT;
import static co.streamx.fluent.SQL.SQL.SELECT;
import static co.streamx.fluent.SQL.SQL.VALUES;
import static co.streamx.fluent.SQL.SQL.WHERE;
import static co.streamx.fluent.SQL.SQL.row;

import java.util.List;

import javax.persistence.EntityManager;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import co.streamx.fluent.JPA.FluentJPA;
import co.streamx.fluent.JPA.FluentQuery;
import co.streamx.fluent.JPA.repository.entities.Person;
import co.streamx.fluent.functions.Consumer1;

@Repository
public interface PersonRepository extends CrudRepository<Person, Long>, EntityManagerSupplier {

    default List<Person> getAllByName(String name) {
        Consumer1<Person> sql = (Person p) -> {
            SELECT(p);
            FROM(p);
        };
        FluentQuery query = FluentJPA.SQL((Person p) -> {
            sql.accept(p);
            WHERE(p.getName() == name);
        });

        System.out.println(query);

        return query.createQuery(getEntityManager(), Person.class).getResultList();
    }

    default List<Person> getAll() {
        EntityManager em = getEntityManager();

        return em.createQuery("select p from Person p", Person.class).getResultList();
    }

    default List<Person> getAllNative() {
        FluentQuery query = FluentJPA.SQL((Person p) -> {
            SELECT(p);
            FROM(p);
        });

        return query.createQuery(getEntityManager(), Person.class).getResultList();
    }

    default Person insertDefault(String name,
                                 int age) {
        FluentQuery query = FluentJPA.SQL((Person p) -> {
            INSERT().INTO(viewOf(p, Person::getName, Person::getAge));
            VALUES(row(name, age));

            // RETURNING(p); //PostgreSQL

            // Person inserted = INSERTED(); //SQL Server
            // OUTPUT(inserted);
        });

        int nOfUpdated = query.createQuery(getEntityManager()).executeUpdate();
        if (nOfUpdated <= 0)
            return null;

        query = FluentJPA.SQL((Person p) -> {
            SELECT(p);
            FROM(p);
            WHERE(p.getId() == LAST_INSERT_ID());
        });

        Person person = query.createQuery(getEntityManager(), Person.class).getSingleResult();

        return person;
    }
}
