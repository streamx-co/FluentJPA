package co.streamx.fluent.JPA.repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

public class EntityManagerSupplierImpl implements EntityManagerSupplier {
    @PersistenceContext
    private EntityManager em;

    @Override
    public EntityManager getEntityManager() {
        return em;
    }
}
