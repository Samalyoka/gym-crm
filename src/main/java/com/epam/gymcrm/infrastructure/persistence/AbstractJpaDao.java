package com.epam.gymcrm.infrastructure.persistence;

import com.epam.gymcrm.application.port.out.UsernameLookup;
import com.epam.gymcrm.infrastructure.persistence.entity.TrainingTypeEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Common base for JPA DAOs: holds the EntityManager and the queries shared by several DAOs.
 */
public abstract class AbstractJpaDao implements UsernameLookup {

    protected EntityManager entityManager;

    @PersistenceContext
    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /** Looks through all users (trainees and trainers), because usernames are unique across both. */
    @Override
    public List<String> findUsernamesStartingWith(String prefix) {
        return entityManager.createQuery(
                        "select u.username from UserEntity u where u.username like :prefix", String.class)
                .setParameter("prefix", prefix + "%")
                .getResultList();
    }

    protected TrainingTypeEntity findTrainingType(String name) {
        return entityManager.createQuery(
                        "select t from TrainingTypeEntity t where t.name = :name", TrainingTypeEntity.class)
                .setParameter("name", name)
                .getResultList().stream().findFirst()
                .orElseThrow(() -> new NoSuchElementException("Training type not found: " + name));
    }
}
