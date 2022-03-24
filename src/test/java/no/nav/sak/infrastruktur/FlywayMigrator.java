package no.nav.sak.infrastruktur;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.ClassicConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

public class FlywayMigrator {
    private static final Logger log = LoggerFactory.getLogger(FlywayMigrator.class);
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
