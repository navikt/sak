package no.nav.sak.infrastruktur;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

public class FlywayMigrator {
    private static final Logger log = LoggerFactory.getLogger(FlywayMigrator.class);
    private final DataSource dataSource;
    private String[] locations;

    public FlywayMigrator(DataSource dataSource, String... locations) {
        this.dataSource = dataSource;
        if (locations.length == 0) {
            this.locations = new String[]{"classpath:/db/migration"};
        } else {
            this.locations = locations;
        }

    }

    public void migrate() {
        log.info("Starter database-migrering");
        Flyway flyway = new Flyway();
        flyway.setLocations(locations);
        flyway.setDataSource(dataSource);
        flyway.migrate();
    }
}
