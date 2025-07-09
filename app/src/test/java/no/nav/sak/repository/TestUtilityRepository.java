package no.nav.sak.repository;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.test.context.transaction.TestTransaction;

@Repository
public class TestUtilityRepository {
	private final EntityManager entityManager;
	private final SakJpaRepository sakJpaRepository;

	public TestUtilityRepository(EntityManager entityManager,
								 SakJpaRepository sakJpaRepository) {
		this.entityManager = entityManager;
		this.sakJpaRepository = sakJpaRepository;
	}

	public void resetAfterTest() {
		entityManager.createNativeQuery("truncate table sak").executeUpdate();
		commit();
	}

	public Sak lagre(Sak sak) {
		try {
			return sakJpaRepository.persist(sak);
		} finally {
			commit();
		}
	}

	public static void commit() {
		TestTransaction.flagForCommit();
		TestTransaction.end();
		TestTransaction.start();
	}
}
