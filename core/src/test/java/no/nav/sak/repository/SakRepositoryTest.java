package no.nav.sak.repository;

import no.nav.sak.SakTestConfiguration;
import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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
        String tema = RandomStringUtils.randomAlphabetic(3);
        String orgnr = "974652250";

        Sak sak1 = sakRepository.lagre(new SakTestData().orgnr(orgnr).tema(tema).build());
        sakRepository.lagre(new SakTestData().orgnr(SakTestData.generateValidOrgnr()).build());
        sakRepository.lagre(new SakTestData().aktoerId(randomNumeric(5)).build());
        Sak sak2 = sakRepository.lagre(new SakTestData().orgnr(orgnr).tema(tema).build());

        List<Sak> saker = sakRepository.finnSaker(SakSearchCriteria.create().medTema(Collections.singletonList(tema)).medOrgnr(orgnr));
        Assertions.assertThat(saker).containsOnly(sak1, sak2);
    }
}
