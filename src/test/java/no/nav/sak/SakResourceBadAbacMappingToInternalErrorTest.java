package no.nav.sak;

import static org.assertj.core.api.Assertions.assertThat;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;

public class SakResourceBadAbacMappingToInternalErrorTest extends AbstractSakResourceTest {

    @Test
    void hent_sak_giving_bad_abac_result_mapping_to_internal_error() {

        final Sak opprettetSak =
            sakRepository
                .lagre(
                    new SakTestData()
                        .aktoerId("123")
                        .build()
                );

        final Response response = executeGetRequest(sakRootTarget().path(String.valueOf(opprettetSak.getId())));

        assertThat(response.getStatus()).isEqualTo(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    void opprett_sak_giving_bad_abac_result_mapping_to_internal_error() {

        final Sak sak =
            new SakTestData()
                .aktoerId("1")
                .applikasjon("FS22")
                .build();

        createSakAndTestReponse(sak);
    }

    @Test
    void finn_saker_giving_bad_abac_result_mapping_to_internal_error() {

        opprett100Tilfeldigesaker();
        final String tema = RandomStringUtils.randomAlphabetic(4);
        final Sak sak = sakRepository.lagre(new SakTestData().tema(tema).build());

        final Response response = executeGetRequest(sakRootTarget()
            .queryParam("tema", sak.getTema())
            .queryParam("aktoerId", sak.getAktoerId()));

        assertThat(response.getStatus()).isEqualTo(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Override
    protected Application configure() {
        return new SakJunitApplicationAlwaysGivingBadAbacMappingToInternalError();
    }

    @Override
    protected Response createSakAndTestReponse(final Sak sak) {

        final Response createdResponse =
            executePost(
                Entity
                    .json(new SakJsonTestData(sak).buildJsonString())
            );

        assertThat(createdResponse.getStatus()).isEqualTo(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());

        return createdResponse;
    }
}
