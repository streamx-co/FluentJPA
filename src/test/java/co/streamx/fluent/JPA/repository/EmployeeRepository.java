package co.streamx.fluent.JPA.repository;

import static co.streamx.fluent.SQL.Directives.alias;
import static co.streamx.fluent.SQL.Directives.subQuery;
import static co.streamx.fluent.SQL.SQL.FROM;
import static co.streamx.fluent.SQL.SQL.SELECT;
import static co.streamx.fluent.SQL.SQL.WHERE;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.TypedQuery;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.streamx.fluent.JPA.FluentJPA;
import co.streamx.fluent.JPA.FluentQuery;
import co.streamx.fluent.JPA.repository.entities.Company;
import co.streamx.fluent.JPA.repository.entities.Employee;
import co.streamx.fluent.JPA.repository.entities.EmployeeId;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, EmployeeId>, EntityManagerSupplier {

    @Data
    @EqualsAndHashCode(callSuper = true)
    class Employee1 extends Employee {
        @Column(name = "aa")
        private String nameA;
    }

    default List<Employee1> getAllNative() {
        FluentQuery query = FluentJPA.SQL((Employee p) -> {
            SELECT(p, alias(p.getName(), "aa"));
            FROM(p);
        });

        return query.createQuery(getEntityManager(), Employee1.class).getResultList();
    }

    default List<Employee> getAllNativeWhere(String name) {
        FluentQuery query = FluentJPA.SQL((Employee p,
                                           Company c) -> {

            Company cc = subQuery(() -> {
                SELECT(c);
                FROM(p).JOIN(c).ON(p.getCompany() == c);
            });

            SELECT(p);
            FROM(p).JOIN(cc).ON(p.getCompany() == cc);
            WHERE((p.getName() == name || p.getName() == name || p.getName() == "Vlad Mihal'cea")
                    && p.getCompany() == cc);
        });

        return query.createQuery(getEntityManager(), Employee.class).getResultList();
    }

    default List<Company> getEmployeeCompanies() {
        FluentQuery query = FluentJPA.SQL((Employee p,
                                           Company c) -> {
            SELECT(c);
            FROM(p).JOIN(c).ON(p.getId().getCompanyId() == c);
        });

        TypedQuery<Company> q = query.createQuery(getEntityManager(), Company.class);
        return q.getResultList();
    }

    default List<Company> getEmployeeCompanies1() {
        FluentQuery query = FluentJPA.SQL((Employee p,
                                           Company c) -> {
            SELECT(c);
            FROM(p).JOIN(c).ON(c == p.getId().getCompanyId());
        });

        return query.createQuery(getEntityManager(), Company.class).getResultList();
    }

    default List<Company> getEmployeeCompanies2() {
        FluentQuery query = FluentJPA.SQL((Employee p,
                                           Company c) -> {
            SELECT(c);
            FROM(p).JOIN(c).ON(c == p.getCompany());
        });

        return query.createQuery(getEntityManager(), Company.class).getResultList();
    }
}
