package no.nav.sak.infrastruktur;

import no.nav.sak.SakRepository;
import no.nav.sak.SakSearchCriteria;
import no.nav.sak.SakTestData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class TransactionManagerTest {
    private static final DataSource dataSource = JunitDataSource.get();
    private final Database database = new Database(dataSource);
    private final SakRepository sakRepository = new SakRepository(database);
    private final TransactionManager transactionManager = new TransactionManager(database);

    @BeforeAll
    static void migrate() {
        new FlywayMigrator(dataSource).migrate();
    }

    @Test
    void ruller_tilbake_naar_exception() {
        try {
            transactionManager.doInTransaction(() -> {
                opprettSak();
                database.insert("insert into sak (id) values (?)");
            });
            fail("Skulle ha kastet ex ved insert");
        } catch (DatabaseException e) {
            assertThat(sakRepository.finnSaker(SakSearchCriteria.create())).isEmpty();
        }

    }

    @Test
    void commiter_naar_ok() {
        transactionManager.doInTransaction(() -> {
            opprettSak();
            opprettSak();
        });
        assertThat(sakRepository.finnSaker(SakSearchCriteria.create())).hasSize(2);
        database.execute("delete from sak");
    }

    private void opprettSak() {
        sakRepository.lagre(new SakTestData().aktoerId("1").build());
    }

}
