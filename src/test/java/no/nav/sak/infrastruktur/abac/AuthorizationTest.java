package no.nav.sak.infrastruktur.abac;

import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Header;
import com.jayway.restassured.specification.RequestSpecification;
import no.nav.sak.Sak;
import no.nav.sak.SakConfiguration;
import no.nav.sak.SakJsonTestData;
import no.nav.sak.SakTestData;
import no.nav.sak.infrastruktur.oicd.OidcLogin;
import no.nav.sak.server.DevJetty;
import org.junit.jupiter.api.*;

import static com.jayway.restassured.RestAssured.basePath;
import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

@Tag("integration-test")
class AuthorizationTest {
    private static DevJetty devJetty;

    private static final String NAVRESSURS_TILGANG_ENHET_0101_USERNAME = "Z990703";
    private static final String NAVRESSURS_IKKE_TILGANG_ENHET_0101_USERNAME = "Z990337";
    private static final String NAVRESSURS_TILGANG_KODE_6_USERNAME = "Z990266";
    private static final String NAVRESSURS_IKKE_TILGANG_KODE_6_USERNAME = "Z990267";
    private static final String NAVRESSURS_TILGANG_EGEN_ANSATT_USERNAME = "Z990267";
    private static final String NAVRESSURS_IKKE_TILGANG_EGEN_ANSATT_USERNAME = "Z990266";
    private static final String NAVRESSURS_TILGANG_NASJONAL = "Z990267";
    private static final String NAVRESSURS_TILGANG_REGIONAL = "Z990266";
    private static final String NAVRESSURS_TILGANG_KODE7 = "Z990743";
    private static final String NAVRESSURS_IKKE_TILGANG_KODE7 = "Z990266";
    private static final String SYSTEMBRUKER_UTEN_TILGANG = "srvsak";
    private static final String SYSTEMBRUKER_MED_TILGANG = "srvgsak";


    private SakConfiguration sakConfiguration = new SakConfiguration();
    private String navressurs***passord=gammelt_passord***");

    @BeforeAll
    static void setup() throws Exception {
        System.setProperty("sak.port", "8099");
        devJetty = new DevJetty();
        devJetty.start();
    }

    @Test
    void navressurs_har_tilgang_til_sak_tilhorende_egen_enhet_for_standard_bruker() {
        verifiserSakTilgang(NAVRESSURS_TILGANG_ENHET_0101_USERNAME, navressursPassord, sakConfiguration.getRequiredString("AKTOERID_0101"));
    }

    @Test
    void navressurs_med_tilgang_nasjonal_har_tilgang_til_saker_utenfor_egen_enhet() {
        verifiserSakTilgang(NAVRESSURS_TILGANG_NASJONAL, navressursPassord, sakConfiguration.getRequiredString("AKTOERID_0101"));
    }

    @Test
    void navressurs_med_tilgang_regional_har_tilgang_til_saker_utenfor_egen_enhet() {
        verifiserSakTilgang(NAVRESSURS_TILGANG_REGIONAL, navressursPassord, sakConfiguration.getRequiredString("AKTOERID_0101"));
    }

    @Test
    void navressurs_har_ikke_tilgang_sak_utover_egen_enhet_for_standard_bruker() {
        String aktoerId = sakConfiguration.getRequiredString("AKTOERID_0101");
        Long id = opprettSak(NAVRESSURS_TILGANG_ENHET_0101_USERNAME, navressursPassord, aktoerId);

        verifiserIngenTilgang(NAVRESSURS_IKKE_TILGANG_ENHET_0101_USERNAME, navressursPassord, id, aktoerId);
    }

    @Test
    void navressurs_med_tilgang_kode_6_gir_tilgang_til_saker_koblet_mot_kode_6_bruker() {
        verifiserSakTilgang(NAVRESSURS_TILGANG_KODE_6_USERNAME, navressursPassord, sakConfiguration.getRequiredString("AKTOERID_KODE6"));
    }

    @Test
    void navressurs_uten_tilgang_kode_6_gir_ikke_tilgang_til_saker_koblet_mot_kode_6_bruker() {
        String aktoerId = sakConfiguration.getRequiredString("AKTOERID_KODE6");
        Long id = opprettSak(NAVRESSURS_TILGANG_KODE_6_USERNAME, navressursPassord, aktoerId);

        verifiserIngenTilgang(NAVRESSURS_IKKE_TILGANG_KODE_6_USERNAME, navressursPassord, id, aktoerId);
    }

    @Test
    void navressurs_med_tilgang_egen_ansatt_gir_tilgang_til_saker_koblet_mot_egen_ansatt() {
        verifiserSakTilgang(NAVRESSURS_TILGANG_EGEN_ANSATT_USERNAME, navressursPassord, sakConfiguration.getRequiredString("AKTOERID_EGEN_ANSATT"));
    }

    @Test
    void navressurs_uten_tilgang_egen_ansatt_gir_ikke_tilgang_til_saker_koblet_mot_egen_ansatt_bruker() {
        String aktoerId = sakConfiguration.getRequiredString("AKTOERID_EGEN_ANSATT");
        Long id = opprettSak(NAVRESSURS_TILGANG_EGEN_ANSATT_USERNAME, navressursPassord, aktoerId);

        verifiserIngenTilgang(NAVRESSURS_IKKE_TILGANG_EGEN_ANSATT_USERNAME, navressursPassord, id, aktoerId);
    }

    @Test
    void navressurs_med_tilgang_kode_7_gir_tilgang_til_saker_koblet_mot_kode_7_bruker() {
        verifiserSakTilgang(NAVRESSURS_TILGANG_KODE7, navressursPassord, sakConfiguration.getRequiredString("AKTOERID_KODE7"));
    }

    @Test
    void navressurs_uten_tilgang_kode_7_gir_ikke_tilgang_til_saker_koblet_mot_kode_7_bruker() {
        String aktoerId = sakConfiguration.getRequiredString("AKTOERID_KODE7");
        Long id = opprettSak(NAVRESSURS_TILGANG_KODE7, navressursPassord, aktoerId);
        verifiserIngenTilgang(NAVRESSURS_IKKE_TILGANG_KODE7, navressursPassord, id, aktoerId);
    }

    @Test
    void systembruker_uten_tilgangsrolle_har_ikke_tilgang_til_saker() {
        String aktoerId = sakConfiguration.getRequiredString("AKTOERID_0101");
        Long id = opprettSak(NAVRESSURS_TILGANG_ENHET_0101_USERNAME, navressursPassord, aktoerId);
        verifiserIngenTilgang(SYSTEMBRUKER_UTEN_TILGANG, sakConfiguration.getRequiredString("SYSTEMBRUKER_UTEN_TILGANG_PASSWORD"), id, aktoerId);
    }

    @Test
    void systembruker_med_tilgangsrolle_gir_tilgang_til_alle_saker() {
        verifiserSakTilgang(SYSTEMBRUKER_MED_TILGANG, sakConfiguration.getRequiredString("SYSTEMBRUKER_MED_TILGANG_PASSWORD"), sakConfiguration.getRequiredString("AKTOERID_EGEN_ANSATT"));
    }

    @Test
    @Disabled("TODO")
    void ekstern_bruker_har_bare_tilgang_til_egne_saker() {

    }

    private void verifiserSakTilgang(String username, String password, String aktoerId) {
        Sak sak = new SakTestData()
            .aktoerId(aktoerId)
            .build();
        SakJsonTestData sakJsonTestData = new SakJsonTestData(sak);

        RequestSpecification spec = oidc(username, password);
        Long id = spec
            .body(sakJsonTestData.buildJsonString()).post("api/v1/saker").then().statusCode(201)
            .extract().jsonPath().getLong("id");

        Object aktoerID = oidc(username, password).pathParam("id", id).get("api/v1/saker/{id}").then().statusCode(200).extract().path("aktoerId");
        oidc(username, password).queryParam("aktoerId", aktoerID).get("api/v1/saker").then().statusCode(200).body("aktoerId", hasItem(aktoerId));
    }

    private void verifiserIngenTilgang(String usernameIkkeTilgang, String passwordIkkeTilgang, Long oppgaveId, String aktoerid) {
        Sak sak = new SakTestData()
            .aktoerId(aktoerid)
            .build();
        oidc(usernameIkkeTilgang, passwordIkkeTilgang).body(new SakJsonTestData(sak).buildJsonString())
            .post("api/v1/saker").then().statusCode(403);

        oidc(usernameIkkeTilgang, passwordIkkeTilgang).pathParam("id", oppgaveId).get("api/v1/saker/{id}").then().statusCode(403);
        oidc(usernameIkkeTilgang, passwordIkkeTilgang).queryParam("aktoerId", aktoerid).get("api/v1/saker").then().statusCode(200).body("aktoerId", not(hasItem(aktoerid)));
    }

    private Long opprettSak(String username, String password, String aktoerId) {
        SakTestData sakTestData = new SakTestData()
            .aktoerId(aktoerId);
        return opprettSak(username, password, sakTestData);
    }

    private Long opprettSak(String username, String password, SakTestData sakTestData) {
        return oidc(username, password)
            .body(new SakJsonTestData(sakTestData.build()).buildJsonString()).post("api/v1/saker").then()
            .extract().jsonPath().getLong("id");
    }

    private RequestSpecification oidc(String username, String password) {
        return baseSpec()
            .header(new Header("Authorization", "Bearer " + new OidcLogin().getIdToken(username, password)));
    }

    private RequestSpecification baseSpec() {
        return given().port(8099)
            .header("X-Correlation-ID", "Junit")
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON);
    }

    @AfterAll
    static void shutdown() throws Exception {
        devJetty.shutdown();
    }
}
