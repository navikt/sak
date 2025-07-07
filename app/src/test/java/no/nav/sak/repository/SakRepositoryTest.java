package no.nav.sak.repository;

import jakarta.annotation.Resource;
import no.nav.sak.SakTestConfiguration;
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server;
import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

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
@EnableMockOAuth2Server
@Transactional
class SakRepositoryTest {
    @Resource
    SakRepository sakRepository;

    @Test
    void henter_sak_med_en_gitt_id() {
        Sak test = new SakTestData().aktoerId("1").build();
        Sak opprettet = test.withId(sakRepository.lagre(test));

        Optional<Sak> hentet = sakRepository.hentSak(opprettet.getId());

        assertThat(hentet.orElseThrow(IllegalStateException::new)).isEqualTo(opprettet);
    }

    @Test
    void oppretter_og_returnerer_opprettet_sak() {
        Long sakId = sakRepository.lagre(new SakTestData().aktoerId("1").build());

        assertThat(sakId).isNotNull();
    }

    @Test
    void finner_saker_for_enkelt_kriterie() {
        String aktoerId = "5";

        sakRepository.lagre(new SakTestData().aktoerId("4").build());
        Sak aktoerIdTwin = new SakTestData().aktoerId(aktoerId).build();
        Long sak1Id = sakRepository.lagre(aktoerIdTwin);
        Long sak2Id = sakRepository.lagre(aktoerIdTwin);
        Sak sak1 = aktoerIdTwin.withId(sak1Id);
        Sak sak2 = aktoerIdTwin.withId(sak2Id);

        List<Sak> saker = sakRepository.finnSaker(SakSearchCriteria.create().medAktoerId(List.of(sak1.getAktoerId())));
        Assertions.assertThat(saker).containsOnly(sak1, sak2);
    }

    @Test
    void finner_saker_for_flere_kriterier() {
        String tema = RandomStringUtils.randomAlphabetic(3);
        String orgnr = "974652250";

        Sak sakLiktOrgnrTema = new SakTestData().orgnr(orgnr).tema(tema).build();
        long sak1Id = sakRepository.lagre(sakLiktOrgnrTema);
        sakRepository.lagre(new SakTestData().orgnr(SakTestData.generateValidOrgnr()).build());
        sakRepository.lagre(new SakTestData().aktoerId(randomNumeric(5)).build());
        long sak2Id = sakRepository.lagre(sakLiktOrgnrTema);
        Sak sak1 = sakLiktOrgnrTema.withId(sak1Id);
        Sak sak2 = sakLiktOrgnrTema.withId(sak2Id);

        List<Sak> saker = sakRepository.finnSaker(SakSearchCriteria.create().medTema(Collections.singletonList(tema)).medOrgnr(orgnr));
        Assertions.assertThat(saker).containsOnly(sak1, sak2);
    }
}
