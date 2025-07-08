package no.nav.sak.repository;

import no.nav.sak.SakTestConfiguration;
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server;
import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static no.nav.sak.repository.TestUtilityRepository.commit;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("itest")
@SpringBootTest(
        classes = SakTestConfiguration.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@EnableMockOAuth2Server
@Transactional
class SakJpaRepositoryTest {
    @Autowired
    SakJpaRepository sakJpaRepository;
    @Autowired
    protected TestUtilityRepository testUtilityRepository;

    @AfterEach
    void tearDown() {
        testUtilityRepository.resetAfterTest();
    }

    @Test
    void henter_sak_med_en_gitt_id() {
        Sak test = new SakTestData().aktoerId("1").build();
        Sak opprettet = sakJpaRepository.persist(test);

        Optional<Sak> hentet = sakJpaRepository.findById(opprettet.getSakId());

        assertThat(hentet.orElseThrow(IllegalStateException::new)).isEqualTo(opprettet);
    }

    @Test
    void oppretter_og_returnerer_opprettet_sak() {
        Sak sak = sakJpaRepository.persist(new SakTestData().aktoerId("1").build());

        assertThat(sak.getSakId()).isNotNull();
    }

    @Test
    void finner_saker_for_enkelt_kriterie() {
        String aktoerId = "5";

        sakJpaRepository.persist(new SakTestData().aktoerId("4").build());
        Sak aktoerIdTwin = new SakTestData().aktoerId(aktoerId).build();
        Sak sak1 = sakJpaRepository.persist(aktoerIdTwin);
        Sak sak2 = sakJpaRepository.persist(aktoerIdTwin);
        commit();

        List<Sak> saker = sakJpaRepository.finnSaker(SakSearchCriteria.create().medAktoerId(List.of(sak1.getAktoerId())));
        Assertions.assertThat(saker).containsOnly(sak1, sak2);
    }

    @Test
    void finner_saker_for_flere_kriterier() {
        String tema = RandomStringUtils.secure().nextAlphabetic(3);
        String orgnr = "974652250";

        Sak sakLiktOrgnrTema = new SakTestData().orgnr(orgnr).tema(tema).build();
        Sak sak1 = sakJpaRepository.persist(sakLiktOrgnrTema);
        sakJpaRepository.persist(new SakTestData().orgnr(SakTestData.generateValidOrgnr()).build());
        sakJpaRepository.persist(new SakTestData().aktoerId(RandomStringUtils.secure().nextNumeric(5)).build());
        Sak sak2 = sakJpaRepository.persist(sakLiktOrgnrTema);
        commit();

        List<Sak> saker = sakJpaRepository.finnSaker(SakSearchCriteria.create().medTema(Collections.singletonList(tema)).medOrgnr(orgnr));
        Assertions.assertThat(saker).containsOnly(sak1, sak2);
    }
}
