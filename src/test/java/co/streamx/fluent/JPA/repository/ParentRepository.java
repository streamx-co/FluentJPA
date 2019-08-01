package co.streamx.fluent.JPA.repository;

import static co.streamx.fluent.SQL.SQL.FROM;
import static co.streamx.fluent.SQL.SQL.SELECT;

import java.util.List;

import javax.persistence.EntityManager;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.streamx.fluent.JPA.FluentJPA;
import co.streamx.fluent.JPA.FluentQuery;
import co.streamx.fluent.JPA.repository.entities.Child;
import co.streamx.fluent.JPA.repository.entities.Parent;

@Repository
public interface ParentRepository extends JpaRepository<Parent, Parent.Key>, EntityManagerSupplier {

    default List<Child> getParentChildrenJPQL() {
        EntityManager em = getEntityManager();

        return em.createQuery("select c from Child c JOIN c.parent", Child.class).getResultList();
    }

    default List<Child> getParentChildren() {
        FluentQuery query = FluentJPA.SQL((Parent p,
                                           Child c) -> {
            SELECT(c);
            FROM(p).JOIN(c).ON(p == c.getParent());
        });

        return query.createQuery(getEntityManager(), Child.class).getResultList();
    }

    default List<Child> getParentChildren2() {
        FluentQuery query = FluentJPA.SQL((Parent p,
                                           Child c) -> {
            SELECT(c);
            FROM(p).JOIN(c).ON(c == p.getChilds());
        });

        return query.createQuery(getEntityManager(), Child.class).getResultList();
    }

    @SuppressWarnings("unchecked")
    default List<String> getParentCodes() {
        FluentQuery query = FluentJPA.SQL((Parent p,
                                           Child c) -> {
            SELECT(c.getParent().getCode());
            FROM(p).JOIN(c).ON(c == p.getChilds());
        });

        return query.createQuery(getEntityManager()).getResultList();
    }
}
