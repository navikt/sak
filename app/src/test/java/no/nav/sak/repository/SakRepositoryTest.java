package no.nav.sak.repository;

import no.nav.sak.SakTestConfiguration;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("itest")
@SpringBootTest(
        classes = SakTestConfiguration.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Transactional
class SakRepositoryTest {
    @Resource
    SakRepository sakRepository;

    @Resource
    TestDatabase database;

    @AfterEach
    void tearDown() {
        database.truncateSakTable();
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

        List<Sak> saker = sakRepository.finnSaker(SakSearchCriteria.create().medAktoerId(List.of(sak1.getAktoerId())));
        Assertions.assertThat(saker).containsOnly(sak1, sak2);
    }

    @Test
    void finner_saker_for_flere_kriterier() {
        String tema = randomAlphabetic(3);
        String orgnr = "974652250";

        Sak sak1 = sakRepository.lagre(new SakTestData().orgnr(orgnr).tema(tema).fagsakNr(randomNumeric(6)).build());
        sakRepository.lagre(new SakTestData().orgnr(SakTestData.generateValidOrgnr()).fagsakNr(randomNumeric(6)).build());
        sakRepository.lagre(new SakTestData().aktoerId(randomNumeric(5)).build());
        Sak sak2 = sakRepository.lagre(new SakTestData().orgnr(orgnr).tema(tema).build());

        List<Sak> saker = sakRepository.finnSaker(SakSearchCriteria.create().medTema(Collections.singletonList(tema)).medOrgnr(orgnr));
        Assertions.assertThat(saker).containsOnly(sak1, sak2);
    }

    @ParameterizedTest
    @CsvSource({
        ",fasgka,974652250,",
        ",fagska,,aktoer10",
        "FS22,,974652250,",
        "FS22,,,aktoer10"
    })
    void finner_saker_for_flere_kriterier_og_duplikater(String applikasjon, String fagsakNr, String orgnr, String aktoerId) {
        Sak sakAnnetOrgnr = sakRepository.lagre(new SakTestData().fagsakNr(randomNumeric(6)).orgnr(SakTestData.generateValidOrgnr()).build());
        Sak sakAnnenAktoer = sakRepository.lagre(new SakTestData().fagsakNr(randomNumeric(6)).aktoerId(randomNumeric(5)).build());
        Sak generellSakAnnetOrgnr = sakRepository.lagre(new SakTestData().applikasjon("FS22").orgnr(SakTestData.generateValidOrgnr()).build());
        Sak generellSakAnnenAktoer = sakRepository.lagre(new SakTestData().applikasjon("FS22").aktoerId(randomNumeric(5)).build());

        String tema = "TEM";
        SakTestData protoSak = new SakTestData().tema(tema).applikasjon(applikasjon).fagsakNr(fagsakNr).aktoerOrOrganisasjon(aktoerId, orgnr);
        Sak sak1 = sakRepository.lagre(protoSak.build());
        Sak sak2 = sakRepository.lagre(new SakTestData().tema(tema).applikasjon(applikasjon).fagsakNr(randomNumeric(6)).aktoerOrOrganisasjon(aktoerId, orgnr).build());
        Sak duplikatSak = sakRepository.lagre(new SakTestData().duplicateOf(protoSak).build());

        List<Sak> saker = sakRepository.finnSaker(SakSearchCriteria.create().medTema(Collections.singletonList(tema)).medOrgnr(orgnr));
        Assertions.assertThat(saker).containsOnly(sak1, sak2);
    }
}
