package no.nav.sak.repository;

import org.springframework.stereotype.Repository;

@Repository
public class TestUtilityRepository {
	private final TestDatabase database;
	private final SakRepository sakRepository;

	public TestUtilityRepository(TestDatabase database) {
		this.database = database;
		this.sakRepository = new SakRepository(database);
	}

	public void resetAfterTest() {
		database.truncateSakTable();
	}

	public Sak lagre(Sak sak) {
		return sakRepository.lagre(sak);
	}
}
