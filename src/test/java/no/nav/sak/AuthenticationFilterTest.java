package no.nav.sak;

import no.nav.sak.infrastruktur.authentication.saml.SAMLSupport;
import no.nav.sak.infrastruktur.oicd.JwtClaimsTestData;
import no.nav.sak.infrastruktur.oicd.JwtTestData;
import org.glassfish.jersey.test.JerseyTest;
import org.joda.time.DateTime;
import org.jose4j.jwt.JwtClaims;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import static no.nav.sikkerhet.authentication.AuthenticationHeaderIdentifier.SAML;
import static org.assertj.core.api.Assertions.assertThat;

public class AuthenticationFilterTest extends JerseyTest {
    private static String samlToken;
    private static SAMLSupport samlSupport;

    @BeforeAll
    static void setup() {
        SakConfiguration sakConfiguration = new SakConfiguration();
        samlSupport = new SAMLSupport(sakConfiguration);
        samlToken = samlSupport.createNewToken();
    }

    @BeforeEach
    void before() throws Exception {
        super.setUp();
    }

    @Override
    protected Application configure() {
        return new SakJunitApplication();
    }


    @Test
    void skal_nektes_adgang_uten_auth_header() {
        skal_nektes_adgang_med_header("", "");
    }

    @Test
    void skal_nektes_adgang_uten_gyldig_saml_token() {
        skal_nektes_adgang_med_header("Authorization", "Invalidtoken");
    }

    @Test
    void skal_nektes_adgang_uten_gyldig_header() {
        skal_nektes_adgang_med_header("Auth", samlToken);
    }

    @Test
    void skal_nektes_adgang_uten_gyldig_auth_header_identifier() {
        skal_nektes_adgang_med_header("Authorization", "Invalididentifier" + " " + samlToken);
    }

    private void skal_nektes_adgang_med_header(String headerName, String headerValue) {
        Response response = sakRootTarget().path("1").request()
            .header("X-Correlation-ID", "Junit")
            .header(headerName, headerValue).get();
        assertThat(response.getStatus()).isEqualTo(Response.Status.UNAUTHORIZED.getStatusCode());

        response = sakRootTarget().request()
            .header("X-Correlation-ID", "Junit")
            .header(headerName, headerValue).get();
        assertThat(response.getStatus()).isEqualTo(Response.Status.UNAUTHORIZED.getStatusCode());

        response = sakRootTarget().request()
            .header("X-Correlation-ID", "Junit")
            .header(headerName, headerValue).post(Entity.json(
                new SakJsonTestData(new SakTestData().build()).buildJsonString()));
        assertThat(response.getStatus()).isEqualTo(Response.Status.UNAUTHORIZED.getStatusCode());
    }

    @Test
    void skal_nektes_adgang_naar_token_enda_ikke_gyldig() {
        String token = samlSupport.createNewToken(DateTime.now().plusDays(1), DateTime.now().plusDays(1));

        String header = SAML.getValue() + " " + token;
        Response response = sakRootTarget().request()
            .header("X-Correlation-ID", "Junit")
            .header("Authorization", header).get();
        assertThat(response.getStatus()).isEqualTo(Response.Status.UNAUTHORIZED.getStatusCode());
    }

    @Test
    void skal_nektes_adgang_naar_token_er_expired() {
        String token = samlSupport.createNewToken(DateTime.now().minusDays(2), DateTime.now().minusDays(1));

        String header = SAML.getValue() + " " + token;
        Response response = sakRootTarget().request()
            .header("X-Correlation-ID", "Junit")
            .header("Authorization", header).get();
        assertThat(response.getStatus()).isEqualTo(Response.Status.UNAUTHORIZED.getStatusCode());
    }

    @Test
    void beskyttede_ressurser_ikke_tilgjengelige_naar_ugyldig_authheader_for_oidc() throws Exception {
        Sak sak = new SakTestData().build();
        Entity<String> json = Entity.json(
            new SakJsonTestData(sak).buildJsonString());
        JwtClaims jwtClaims = new JwtClaimsTestData().build();
        jwtClaims.unsetClaim("azp");
        String authHeader = "Bearer " + new JwtTestData()
            .claims(jwtClaims)
            .build();

        Response opprettResponse = sakRootTarget()
            .request()
            .header("X-Correlation-ID", "Junit")
            .header("Authorization", authHeader)
            .post(json);
        assertThat(opprettResponse.getStatus()).isEqualTo(Response.Status.UNAUTHORIZED.getStatusCode());

        Response searchResponse = sakRootTarget()
            .request()
            .header("X-Correlation-ID", "Junit")
            .header("Authorization", authHeader)
            .get();
        assertThat(searchResponse.getStatus()).isEqualTo(Response.Status.UNAUTHORIZED.getStatusCode());

        Response getResponse = sakRootTarget().path("1")
            .request()
            .header("X-Correlation-ID", "Junit")
            .header("Authorization", authHeader)
            .get();
        assertThat(getResponse.getStatus()).isEqualTo(Response.Status.UNAUTHORIZED.getStatusCode());
    }


    private WebTarget sakRootTarget() {
        return target("/v1/saker");
    }

}
