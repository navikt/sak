package no.nav.sak.repository;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.util.Arrays;

public class TestDatabase extends Database {

	public TestDatabase(DataSource dataSource) {
		super(dataSource);
	}

	void execute(String query, Object... parameters) {
		executeDbOperation(query, Arrays.asList(parameters), PreparedStatement::executeUpdate);
	}

	void truncateSakTable() {
		execute("truncate table sak;");
	}
}