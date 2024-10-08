package co.streamx.fluent.JPA.repository;

import jakarta.persistence.EntityManager;

public interface EntityManagerSupplier {
    EntityManager getEntityManager();
}
