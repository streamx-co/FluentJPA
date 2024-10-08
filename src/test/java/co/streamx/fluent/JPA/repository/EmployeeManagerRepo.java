package co.streamx.fluent.JPA.repository;

import static co.streamx.fluent.SQL.Directives.alias;
import static co.streamx.fluent.SQL.SQL.BY;
import static co.streamx.fluent.SQL.SQL.FROM;
import static co.streamx.fluent.SQL.SQL.ORDER;
import static co.streamx.fluent.SQL.SQL.SELECT;
import static co.streamx.fluent.SQL.ScalarFunctions.CONCAT;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.streamx.fluent.JPA.FluentJPA;
import co.streamx.fluent.JPA.FluentQuery;
import co.streamx.fluent.JPA.repository.entities.EmployeeManager;
import lombok.Data;

@Repository
public interface EmployeeManagerRepo extends JpaRepository<EmployeeManager, Integer>, EntityManagerSupplier {

    @Entity
    @Data
    class EmployeeManagerPair {
        @Id
        private String employee;
        private String manager;
    }

    static String GetFullName(EmployeeManager em) {
        return CONCAT(em.getFirstName(), " ", em.getLastName());
    }

    default List<EmployeeManagerPair> getEManagers() {
        FluentQuery sql = FluentJPA.SQL((EmployeeManager e,
                                         EmployeeManager m) -> {

            String manager = alias(GetFullName(m), EmployeeManagerPair::getManager);

            SELECT(alias(GetFullName(e), EmployeeManagerPair::getEmployee), manager);
            FROM(e).JOIN(m).ON(e.getManager() == m);
            ORDER(BY(manager));

        });

        return sql.createQuery(getEntityManager(), EmployeeManagerPair.class).getResultList();
    }
}
