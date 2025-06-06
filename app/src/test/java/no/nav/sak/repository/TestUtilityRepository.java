package no.nav.sak.repository;

import org.springframework.stereotype.Repository;

@Repository
public class TestUtilityRepository {
	private final Database database;
	private final SakRepository sakRepository;

	public TestUtilityRepository(Database database) {
		this.database = database;
		this.sakRepository = new SakRepository(database);
	}

	public void resetAfterTest() {
		database.execute("truncate table sak;");
	}

	public Sak lagre(Sak sak) {
		return sak.withId(sakRepository.lagre(sak));
	}
}
