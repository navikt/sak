package no.nav.sak.repository;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

import java.util.Optional;

/**
 * Base JPA repository interface.
 * <p>
 * Inneholder metoder som er tilgjengelig for utviklere i produksjonskode.
 * Kan utvides med flere metoder som burde være felles for entitet repositories.
 *
 * @param <T>  JPA entiteten
 * @param <ID> ID datatypen
 */
@NoRepositoryBean
public interface BaseJpaRepository<T, ID> extends Repository<T, ID> {
	/**
	 * Henter entitet basert på id.
	 * <p>
	 * Bruk denne hvis du behøver tilgang til feltene på entiteten.
	 *
	 * @param id kan ikke være {@literal null}.
	 * @return Entitet med id eller {@literal Optional#empty()} hvis den ikke finnes.
	 * @throws IllegalArgumentException hvis {@literal id} er {@literal null}.
	 */
	Optional<T> findById(ID id);
}
