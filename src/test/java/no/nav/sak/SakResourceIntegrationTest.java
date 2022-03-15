package no.nav.sak;


import io.restassured.http.Header;
import no.nav.sak.infrastruktur.oicd.OidcLogin;
import no.nav.sak.server.DevJetty;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration-test")
class SakResourceIntegrationTest {
    private static DevJetty devJetty;
    private SakConfiguration sakConfiguration = new SakConfiguration();

    @BeforeAll
    static void setup() throws Exception {
        System.setProperty("sak.port", "8099");
        devJetty = new DevJetty();
        devJetty.start();
    }

    @AfterAll
    static void shutdown() throws Exception {
        devJetty.shutdown();
    }

    @Test
    void gis_tilgang_med_oidc_auth() throws Exception {
        Header authorizationHeader = new Header("Authorization",
            "Bearer" + " " + new OidcLogin().getIdToken());
        assertThat(given().port(8099)
            .header("X-Correlation-ID", "Junit")
            .header(authorizationHeader).get("api/v1/saker?aktoerId=123").getStatusCode()).isEqualTo(200);
    }

    @Test
    void gis_tilgang_med_basic_auth() throws Exception {
        String username = sakConfiguration.getRequiredString("SRVSAK_USERNAME");
        String password = sakConfiguration.getRequiredString("SRVSAK_PASSWORD");
        String unencoded = username + ":" + password;
        String authHeaderBasic = "Basic " + Base64.getEncoder().encodeToString(unencoded.getBytes("utf-8"));
        Header authorizationHeader = new Header("Authorization",
            authHeaderBasic);
        assertThat(given().port(8099)
            .header("X-Correlation-ID", "Junit")
            .header(authorizationHeader).get("api/v1/saker?aktoerId=123").getStatusCode()).isEqualTo(200);
    }

    @Test
    void nektes_tilgang_uten_auth() throws Exception {
        assertThat(given().port(8099)
            .header("X-Correlation-ID", "Junit")
            .get("api/v1/saker")
            .getStatusCode()).isEqualTo(401);
    }
}
