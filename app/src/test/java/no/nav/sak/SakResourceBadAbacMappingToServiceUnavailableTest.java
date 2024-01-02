package no.nav.sak;

import no.nav.sak.repository.Sak;
import no.nav.sak.repository.SakTestData;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled
public class SakResourceBadAbacMappingToServiceUnavailableTest extends AbstractSakResourceTest {

    @Test
    void hent_sak_giving_bad_abac_result_mapping_to_service_unavailable() {

        final Sak opprettetSak =
            testUtilityRepository
                .lagre(
                    new SakTestData()
                        .aktoerId("123")
                        .build()
                );

        final Response response = executeGetRequest(sakRootTarget().path(String.valueOf(opprettetSak.getId())));

        assertThat(response.getStatus()).isEqualTo(Response.Status.SERVICE_UNAVAILABLE.getStatusCode());
    }

    @Test
    void opprett_sak_giving_bad_abac_result_mapping_to_service_unavailable() {

        final Sak sak =
            new SakTestData()
                .aktoerId("1")
                .applikasjon("FS22")
                .build();

        createSakAndTestReponse(sak);
    }

    @Test
    void finn_saker_giving_bad_abac_result_mapping_to_service_unavailable() {

        opprett100Tilfeldigesaker();
        final String tema = RandomStringUtils.randomAlphabetic(4);
        final Sak sak = testUtilityRepository.lagre(new SakTestData().tema(tema).build());

        final Response response = executeGetRequest(sakRootTarget()
            .queryParam("tema", sak.getTema())
            .queryParam("aktoerId", sak.getAktoerId()));

        assertThat(response.getStatus()).isEqualTo(Response.Status.SERVICE_UNAVAILABLE.getStatusCode());
    }

    @Override
    protected Response createSakAndTestReponse(final Sak sak) {

        final Response createdResponse =
            executePost(
                Entity
                    .json(new SakJsonTestData(sak).buildJsonString())
            );

        assertThat(createdResponse.getStatus()).isEqualTo(Response.Status.SERVICE_UNAVAILABLE.getStatusCode());

        return createdResponse;
    }
}
