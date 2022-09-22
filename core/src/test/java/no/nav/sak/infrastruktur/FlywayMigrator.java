package no.nav.sak.infrastruktur;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.ClassicConfiguration;

import javax.sql.DataSource;

@Slf4j
public class FlywayMigrator {
    private final ClassicConfiguration configuration;

    public FlywayMigrator(DataSource dataSource, String... locations) {
        this.configuration = new ClassicConfiguration();
        this.configuration.setDataSource(dataSource);
        this.configuration.setLocationsAsStrings(locations.length == 0 ? new String[]{"classpath:/db/migration"} : locations);
    }

    public void migrate() {
        log.info("Starter database-migrering");
        Flyway flyway = new Flyway(configuration);
        flyway.migrate();
    }
}
