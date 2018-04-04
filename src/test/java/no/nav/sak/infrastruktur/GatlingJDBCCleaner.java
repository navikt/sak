package no.nav.sak.infrastruktur;

import com.zaxxer.hikari.HikariDataSource;
import no.nav.sak.SakConfiguration;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;

public class GatlingJDBCCleaner {
    private Database database;

    public GatlingJDBCCleaner() {
        HikariDataSource dataSource = new HikariDataSource();
        SakConfiguration sakConfiguration = new SakConfiguration();
        dataSource.setUsername(sakConfiguration.getRequiredString("sakds.lasttest.user"));
        dataSource.setPassword(sakConfiguration.getRequiredString("sakds.lasttest.password"));
        dataSource.setJdbcUrl(sakConfiguration.getRequiredString("sakds.lasttest.url"));
        database = new Database(dataSource);
    }

    public void resetState() {

        try {
            URI uri = GatlingJDBCCleaner.class.getResource("/data/orgnr.csv").toURI();
            Files.readAllLines(Paths.get(uri)).stream()
                .skip(1)
                .map(s -> s.replaceAll(",", ""))
                .forEach(this::deleteSak);
        } catch (Exception e) {
            throw new IllegalStateException("Kunne ikke resette databasetilstand for gatling-tester", e);
        }
    }

    private void deleteSak(String orgnr) {
        database.execute("delete from sak where orgnr = ?", orgnr);
    }

}
