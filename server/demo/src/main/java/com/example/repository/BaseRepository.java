package com.example.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class BaseRepository<T, ID> {

    protected final EntityManagerFactory emf;
    private final Class<T> entityClass;

    public BaseRepository(EntityManagerFactory emf, Class<T> entityClass) {
        this.emf = emf;
        this.entityClass = entityClass;
    }

    public T save(T entity) {
        return executeInsideTransaction(entityManager -> {
            if (entityManager.contains(entity)) {
                return entityManager.merge(entity);
            } else {
                entityManager.persist(entity);
                return entity;
            }
        });
    }

    public Optional<T> findById(ID id) {
        return Optional.ofNullable(execute(entityManager -> entityManager.find(entityClass, id)));
    }

    public List<T> findAll() {
        return execute(entityManager ->
                entityManager.createQuery("FROM " + entityClass.getSimpleName(), entityClass).getResultList());
    }

    public void deleteById(ID id) {
        executeInsideTransaction(entityManager -> {
            T entity = entityManager.find(entityClass, id);
            if (entity != null) {
                entityManager.remove(entity);
            }
            return null;
        });
    }

    protected <R> R execute(Function<EntityManager, R> action) {
        EntityManager em = emf.createEntityManager();
        try {
            return action.apply(em);
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
    }

    protected <R> R executeInsideTransaction(Function<EntityManager, R> action) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            R result = action.apply(em);
            tx.commit();
            return result;
        } catch (RuntimeException e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }
}