package no.nav.sak.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Utdrag fra HibernateRepositoryImpl https://github.com/vladmihalcea/hibernate-types
 *
 * @param <T> JPA entiteten.
 */
public class HibernateRepositoryImpl<T> implements HibernateRepository<T> {

	@PersistenceContext
	private EntityManager entityManager;

	@Override
	public <S extends T> S persist(S entity) {
		entityManager.persist(entity);
		return entity;
	}

}
