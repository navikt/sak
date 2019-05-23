package no.nav.sak;

import no.nav.sak.infrastruktur.Database;
import no.nav.sak.infrastruktur.FlywayMigrator;
import no.nav.sak.infrastruktur.JunitDataSource;
import no.nav.sak.infrastruktur.JunitTransactionSupport;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static org.assertj.core.api.Assertions.assertThat;

class SakRepositoryTest {
    private static final DataSource dataSource = JunitDataSource.get();
    private final Database database = new Database(dataSource);
    private final SakRepository sakRepository = new SakRepository(database);
    private final JunitTransactionSupport junitTransactionSupport = new JunitTransactionSupport(database);

    @BeforeAll
    static void migrate() {
        new FlywayMigrator(dataSource, "classpath:db/migration", "classpath:db/h2/migration").migrate();
    }

    @BeforeEach
    void setup() throws SQLException {
        junitTransactionSupport.initTransaction();
    }

    @Test
    void henter_sak_med_en_gitt_id() {
        Sak opprettet = sakRepository.lagre(new SakTestData().aktoerId("1").build());

        Optional<Sak> hentet = sakRepository.hentSak(opprettet.getId());

        assertThat(hentet.orElseThrow(IllegalStateException::new)).isEqualTo(opprettet);
    }

    @Test
    void oppretter_og_returnerer_opprettet_sak() {
        Sak sak = sakRepository.lagre(new SakTestData().aktoerId("1").build());

        assertThat(sak).isNotNull();
    }

    @Test
    void finner_saker_for_enkelt_kriterie() {
        String aktoerId = randomNumeric(5);

        sakRepository.lagre(new SakTestData().aktoerId(randomNumeric(5)).build());
        Sak sak1 = sakRepository.lagre(new SakTestData().aktoerId(aktoerId).build());
        Sak sak2 = sakRepository.lagre(new SakTestData().aktoerId(aktoerId).build());

        List<Sak> saker = sakRepository.finnSaker(SakSearchCriteria.create().medAktoerId(sak1.getAktoerId()));
        assertThat(saker).containsOnly(sak1, sak2);
    }

    @Test
    void finner_saker_for_flere_kriterier() {
        String tema = RandomStringUtils.randomAlphabetic(3);
        String orgnr = "974652250";

        Sak sak1 = sakRepository.lagre(new SakTestData().orgnr(orgnr).tema(tema).build());
        sakRepository.lagre(new SakTestData().orgnr(SakTestData.generateValidOrgnr()).build());
        sakRepository.lagre(new SakTestData().aktoerId(randomNumeric(5)).build());
        Sak sak2 = sakRepository.lagre(new SakTestData().orgnr(orgnr).tema(tema).build());

        List<Sak> saker = sakRepository.finnSaker(SakSearchCriteria.create().medTema(Collections.singletonList(tema)).medOrgnr(orgnr));
        assertThat(saker).containsOnly(sak1, sak2);
    }

    @AfterEach
    void tearDown() throws SQLException {
        junitTransactionSupport.rollback();
    }
}
