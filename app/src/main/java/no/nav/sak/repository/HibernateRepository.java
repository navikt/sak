package no.nav.sak.repository;

/**
 * Inspirert fra HibernateRepository https://github.com/vladmihalcea/hibernate-types
 * <p>
 * Metoder for å behandle Hibernate sine entity state changes.
 *
 * @param <T> JPA entiteten.
 */
public interface HibernateRepository<T> {
	/**
	 * Persisterer entitet og gjør den managed.
	 * <p>
	 * Bruk denne metoden hvis du skal lagre et nytt objekt.
	 *
	 * @param entity som skal persisteres
	 * @param <S>    entitetstype
	 * @return managed entity
	 */
	<S extends T> S persist(S entity);

}
