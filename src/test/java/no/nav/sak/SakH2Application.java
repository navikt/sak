package no.nav.sak;

import no.nav.sak.infrastruktur.FlywayMigrator;
import no.nav.sak.infrastruktur.JunitDataSource;

import javax.sql.DataSource;

public class SakH2Application extends SakApplication {
    protected DataSource createSakDataSource(SakConfiguration sakConfiguration) {
        return JunitDataSource.get();
    }

    void migrateDataWarehouse(SakConfiguration sakConfiguration){}

    void migrateSak(DataSource dataSource) {
        new FlywayMigrator(dataSource, "classpath:db/migration").migrate();
    }

}
