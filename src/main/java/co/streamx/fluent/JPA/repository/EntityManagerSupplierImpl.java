package co.streamx.fluent.JPA.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

public class EntityManagerSupplierImpl implements EntityManagerSupplier {
    @PersistenceContext
    private EntityManager em;

    @Override
    public EntityManager getEntityManager() {
        return em;
    }
}
