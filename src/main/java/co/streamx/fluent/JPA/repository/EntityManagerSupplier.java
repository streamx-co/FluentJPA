package co.streamx.fluent.JPA.repository;

import javax.persistence.EntityManager;

public interface EntityManagerSupplier {
    EntityManager getEntityManager();
}
