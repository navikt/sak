package no.nav.sak.repository;

public interface SakJpaRepository extends HibernateRepository<Sak>, BaseJpaRepository<Sak, Long>, CustomSakRepository {
}
