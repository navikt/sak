package no.nav.sak.server;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Header;
import no.nav.sak.SakConfiguration;
import no.nav.sak.infrastruktur.authentication.saml.SAMLSupport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static no.nav.sikkerhet.authentication.Authenticator.SAML;
import static org.assertj.core.api.Assertions.assertThat;

class StartJettyTest {

    private static Header authHeaderSaml;

    private static DevJetty devJetty;

    @BeforeAll
    static void setup() throws Exception {
        System.setProperty("sak.port", "8099");
        SakConfiguration sakConfiguration = new SakConfiguration();
        SAMLSupport samlSupport = new SAMLSupport(sakConfiguration);
        authHeaderSaml = new Header("Authorization", SAML + " " + samlSupport.createNewToken());
        devJetty = new DevJetty();
        devJetty.start();
    }

    @AfterAll
    static void shutdown() throws Exception {
        devJetty.shutdown();
    }

    @Test
    void naisressurser_tilgjengeliggjores() throws Exception {
        assertThat(RestAssured.given().port(8099).get("internal/alive").getStatusCode()).isEqualTo(200);
        assertThat(RestAssured.given().port(8099).get("internal/ready").getStatusCode()).isEqualTo(200);
    }

    @Test
    void metrikker_tilgjengeliggjores() throws Exception {
        RestAssured.given().port(8099)
            .header("X-Correlation-ID", "Junit")
            .header(authHeaderSaml).get("api/v1/saker");
        RestAssured.given().port(8099)
            .header("X-Correlation-ID", "Junit")
            .header(authHeaderSaml).get("api/v1/saker/1");
        RestAssured.given().port(8099)
            .header("X-Correlation-ID", "Junit")
            .header(authHeaderSaml).get("internal/alive");

        assertThat(RestAssured.given().port(8099).get("internal/metrics").getBody().asString()).contains(
            "requests_duration_seconds_bucket{path=\"/api/v1/saker\",queryparams=\"N/A\",method=\"GET\"",
            "requests_duration_seconds_bucket{path=\"/api/v1/saker/{id}\",queryparams=\"N/A\",method=\"GET\""
        );
    }

    @Test
    void swagger_dok_tilgjengelig_uten_header() throws Exception {
        assertThat(RestAssured.given().port(8099)
            .get("/api/swagger.json").getStatusCode()).isEqualTo(200);
    }

}
