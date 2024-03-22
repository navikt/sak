package no.nav.sak;

import no.nav.sak.infrastruktur.abac.ABACJunitClientAlwaysGivingBadAbacMappingToInternalError;
import no.nav.sak.infrastruktur.abac.MockableSakPEP;
import no.nav.sak.repository.Sak;
import no.nav.sak.repository.SakTestData;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

public class SakResourceBadAbacMappingToInternalErrorTest extends AbstractSakResourceTest {


    @MockBean
    MockableSakPEP sakPEP;

    @BeforeEach
    void setupAbacClient() {
        when(sakPEP.executeAbacRequest(any())).then((ar) -> ABACJunitClientAlwaysGivingBadAbacMappingToInternalError.create().execute(null));
    }

    @Test
    void finn_saker_giving_bad_abac_result_mapping_to_internal_error() {

        opprett100Tilfeldigesaker();
        final String tema = RandomStringUtils.randomAlphabetic(4);
        final Sak sak = testUtilityRepository.lagre(new SakTestData().tema(tema).build());

        final ResponseEntity<?> response = executeGetRequestWithSaml(
				UriComponentsBuilder.newInstance()
                .queryParam("tema", sak.getTema())
                .queryParam("aktoerId", sak.getAktoerId())
				.build().toUri()
		);

        assertThat(response.getStatusCode()).isEqualTo(INTERNAL_SERVER_ERROR);
    }

    @Override
    protected ResponseEntity<Object> createSakAndTestReponse(final Sak sak) {

        final ResponseEntity<Object> createdResponse =
                executePostWithSaml(
                                new SakJsonTestData(sak).buildJsonString()
                );

        assertThat(createdResponse.getStatusCode()).isEqualTo(INTERNAL_SERVER_ERROR);

        return createdResponse;
    }
}
