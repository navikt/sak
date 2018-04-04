package no.nav.sak.infrastruktur;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

public class FlywayMigrator {
    private static final Logger log = LoggerFactory.getLogger(FlywayMigrator.class);
    private final DataSource dataSource;

    public FlywayMigrator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void migrate() {
        log.info("Starter database-migrering");
        Flyway flyway = new Flyway();
        flyway.setDataSource(dataSource);
        flyway.migrate();
    }
}
