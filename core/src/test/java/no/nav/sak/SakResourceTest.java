package no.nav.sak;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import no.nav.sak.infrastruktur.oicd.JwtTestData;
import no.nav.sak.repository.Sak;
import no.nav.sak.repository.SakSearchCriteria;
import no.nav.sak.repository.SakTestData;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static no.nav.sak.infrastruktur.authentication.basic.JunitBasicAuthenticator.PASSWORD;
import static no.nav.sak.infrastruktur.authentication.basic.JunitBasicAuthenticator.USERNAME;
import static org.apache.commons.lang3.RandomStringUtils.random;
import static org.assertj.core.api.Assertions.assertThat;

class SakResourceTest extends AbstractSakResourceTest {

    @Test
    void henter_sak_for_gitt_id() {

        final Sak opprettetSak =
            testUtilityRepository
                .lagre(
                    new SakTestData()
                        .aktoerId("123")
                        .build()
                );

        Response response = executeGetRequest(sakRootTarget().path(String.valueOf(opprettetSak.getId())));

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(response.getHeaderString("Content-Type")).isEqualTo("application/json");

        SakJson sakJson =  new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (jsonElement, type, jsonDeserializationContext) -> ZonedDateTime.parse(jsonElement.getAsJsonPrimitive().getAsString()).toLocalDateTime())
            .create().fromJson(response.readEntity(String.class), SakJson.class);
        verifyEqual(sakJson, opprettetSak);
    }

    @Test
    void gir_404_naar_sak_ikke_finnes_for_gitt_id() {
        Response response = executeGetRequest(sakRootTarget().path("1"));

        JsonObject jsonObject = JsonParser.parseString(response.readEntity(String.class)).getAsJsonObject();
        assertThat(jsonObject.get("feilmelding").getAsString()).isNotBlank();
        assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    void gir_404_naar_ressurs_ikke_finnes() {
        Response response = executeGetRequest(sakRootTarget().path("/v1/finnesikke/1"));
        assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    void gir_405_naar_operasjon_ikke_tillatt() {
        Response response = sakRootTarget().path("1")
            .request()
            .header("Authorization", authHeaderSaml)
            .header("X-Correlation-ID", correlationId)
            .post(Entity.json(
                new SakJsonTestData(new SakTestData().build()).buildJsonString()));

        assertThat(response.getStatus()).isEqualTo(Response.Status.METHOD_NOT_ALLOWED.getStatusCode());
    }

    @Test
    void oppretter_sak_for_aktoer() {
        Sak sak = new SakTestData()
            .aktoerId("1")
            .build();
        JsonObject jsonObject = createAndRetrieveAtLocation(sak);
        assertThat(jsonObject.get("id").getAsLong()).isNotNull();
        assertThat(jsonObject.get("aktoerId").getAsString()).isEqualTo(sak.getAktoerId());
        assertThat(jsonObject.get("orgnr").isJsonNull()).isTrue();
    }

    @Test
    void gir_400_naar_hverken_aktoer_eller_organisasjon_er_utfylt_ved_opprettelse_av_sak() {
        Response createdResponse = executePost(Entity.json(new SakJsonTestData()
            .medApplikasjon(random(3))
            .medTema(random(3))
            .buildJsonString()));
        verify400(createdResponse);
    }

    @Test
    void beskyttede_ressurser_tilgjengelige_naar_gyldig_authheader_for_oidc() {
        String authHeaderOIDC = "Bearer " + new JwtTestData().build();
        verifyBeskyttedeRessurserTilgjengelig(authHeaderOIDC);
    }

    @Test
    void beskyttede_ressurser_tilgjengelig_naar_gyldig_basic_auth_header() {
        String unencoded = USERNAME + ":" + PASSWORD;
        String authHeaderBasic = "Basic " + Base64.getEncoder().encodeToString(unencoded.getBytes(StandardCharsets.UTF_8));
        verifyBeskyttedeRessurserTilgjengelig(authHeaderBasic);
    }

    @Test
    void oppretter_sak_for_organisasjon() {
        Sak sak = new SakTestData()
            .orgnr(SakTestData.generateValidOrgnr())
            .build();
        JsonObject jsonObject = createAndRetrieveAtLocation(sak);
        assertThat(jsonObject.get("id").getAsLong()).isNotNull();
        assertThat(jsonObject.get("orgnr").getAsString()).isEqualTo(sak.getOrgnr());
        assertThat(jsonObject.get("aktoerId").isJsonNull()).isTrue();
    }

    @Test
    void gir_konflikt_og_oppretter_ikke_ny_sak_dersom_fagsak_finnes_fra_foer() {
        Sak sak = new SakTestData()
            .aktoerId("123")
            .fagsakNr("321")
            .applikasjon("Gosys")
            .build();
        Entity<String> json = Entity.json(
            new SakJsonTestData(sak).buildJsonString());

        Response firstResponse = executePost(json);

        assertThat(firstResponse.getStatus()).isEqualTo(Response.Status.CREATED.getStatusCode());
        assertThat(firstResponse.getHeaderString("Content-Type")).isEqualTo("application/json");

        Response secondResponse = executePost(json);

        assertThat(secondResponse.getStatus()).isEqualTo(Response.Status.CONFLICT.getStatusCode());
        assertThat(secondResponse.getHeaderString("Content-Type")).isEqualTo("application/json");

        assertThat(sakRepository.finnSaker(SakSearchCriteria.create())).hasSize(1);
    }

    @Test
    void oppretter_ny_sak_dersom_fagsak_finnes_fra_foer_men_med_annet_tema() {
        final String AKTOER_ID = "123";
        final String FAGSAK_NR = "321";
        final String APPLIKASJON = "Gosys";
        final String TEMA_1 = "TEMA1";
        final String TEMA_2 = "TEMA2";

        Sak sak1 = new SakTestData()
                .aktoerId(AKTOER_ID)
                .fagsakNr(FAGSAK_NR)
                .applikasjon(APPLIKASJON)
                .tema(TEMA_1)
                .build();
        Entity<String> jsonSak1 = Entity.json(
                new SakJsonTestData(sak1).buildJsonString());

        Sak sak2 = new SakTestData()
                .aktoerId(AKTOER_ID)
                .fagsakNr(FAGSAK_NR)
                .applikasjon(APPLIKASJON)
                .tema(TEMA_2)
                .build();
        Entity<String> jsonSak2 = Entity.json(
                new SakJsonTestData(sak2).buildJsonString());

        Response firstResponse = executePost(jsonSak1);

        assertThat(firstResponse.getStatus()).isEqualTo(Response.Status.CREATED.getStatusCode());
        assertThat(firstResponse.getHeaderString("Content-Type")).isEqualTo("application/json");

        Response secondResponse = executePost(jsonSak2);

        assertThat(secondResponse.getStatus()).isEqualTo(Response.Status.CREATED.getStatusCode());
        assertThat(secondResponse.getHeaderString("Content-Type")).isEqualTo("application/json");

        assertThat(sakRepository.finnSaker(SakSearchCriteria.create()))
                .hasSize(2)
                .allMatch(sak -> sak.getFagsakNr().equals(FAGSAK_NR))
                .allMatch(sak -> sak.getAktoerId().equals(AKTOER_ID))
                .allMatch(sak -> sak.getApplikasjon().equals(APPLIKASJON))
                .extracting(Sak::getTema).containsOnly(TEMA_1, TEMA_2);
    }

    @Test
    void oppretter_generell_sak_uten_applikasjon_angitt() {
        Sak sak = new SakTestData()
            .aktoerId("1")
            .applikasjon(null)
            .build();
        JsonObject jsonObject = createAndRetrieveAtLocation(sak);
        assertThat(jsonObject.get("id").getAsLong()).isNotNull();
        assertThat(jsonObject.get("aktoerId").getAsString()).isEqualTo(sak.getAktoerId());
        assertThat(jsonObject.get("orgnr").isJsonNull()).isTrue();
    }

    @Test
    void kan_opprette_sak_med_applikasjon_uten_aa_angi_fagsaknr() {
        Sak sak = new SakTestData()
            .aktoerId("1")
            .applikasjon("FS22")
            .build();
        JsonObject jsonObject = createAndRetrieveAtLocation(sak);
        assertThat(jsonObject.get("id").getAsLong()).isNotNull();
        assertThat(jsonObject.get("aktoerId").getAsString()).isEqualTo(sak.getAktoerId());
        assertThat(jsonObject.get("orgnr").isJsonNull()).isTrue();
    }

    @Test
    void applikasjon_er_paakrevd_for_fagsak() {
        Sak sak = new SakTestData()
            .aktoerId("1")
            .applikasjon(null)
            .fagsakNr("123")
            .build();
        Response createdResponse = executePost(Entity.json(new SakJsonTestData(sak)
            .buildJsonString()));
        verify400(createdResponse);
    }


    @Test
    void soeker_opp_saker_for_aktoer_id() {
        opprett100Tilfeldigesaker();
        String aktoerId = RandomStringUtils.randomNumeric(9);
        Sak sak1 = testUtilityRepository.lagre(new SakTestData().aktoerId(aktoerId).build());
        Sak sak2 = testUtilityRepository.lagre(new SakTestData().aktoerId(aktoerId).build());

        Response response = executeGetRequest(sakRootTarget().queryParam("aktoerId", aktoerId));

        verifySearchResponseMatching(response, asList(sak1, sak2));
    }

    @Test
    void soeker_opp_saker_for_tema() {
        opprett100Tilfeldigesaker();
        String tema = RandomStringUtils.randomAlphabetic(4);
        Sak sak = testUtilityRepository.lagre(new SakTestData().tema(tema).build());

        Response response = executeGetRequest(sakRootTarget()
            .queryParam("tema", sak.getTema())
            .queryParam("aktoerId", sak.getAktoerId()));

        verifySearchResponseMatching(response, singletonList(sak));
    }

    @Test
    void soeker_opp_saker_for_flere_tema() {
        opprett100Tilfeldigesaker();
        String tema1 = RandomStringUtils.randomAlphabetic(4);
        String tema2 = RandomStringUtils.randomAlphabetic(4);
        Sak sak1 = testUtilityRepository.lagre(new SakTestData().tema(tema1).build());
        Sak sak2 = testUtilityRepository.lagre(new SakTestData().tema(tema2)
            .aktoerId(sak1.getAktoerId())
            .build());

        Response response = executeGetRequest(sakRootTarget()
            .queryParam("tema", sak1.getTema())
            .queryParam("tema", sak2.getTema())
            .queryParam("aktoerId", sak1.getAktoerId()));

        verifySearchResponseMatching(response, asList(sak1, sak2));
    }

    @Test
    void soeker_opp_saker_for_fagsaknr() {
        opprett100Tilfeldigesaker();
        String fagsaknr = RandomStringUtils.randomNumeric(9);
        Sak sak = testUtilityRepository.lagre(new SakTestData().
            applikasjon(RandomStringUtils.randomAlphabetic(3)).
            fagsakNr(fagsaknr).build());

        Response response = executeGetRequest(sakRootTarget()
            .queryParam("fagsakNr", sak.getFagsakNr()));
        verifySearchResponseMatching(response, singletonList(sak));
    }

    @Test
    void soeker_opp_saker_for_orgnr() {
        opprett100Tilfeldigesaker();
        String orgnr = "974652250";
        Sak sak = testUtilityRepository.lagre(new SakTestData().orgnr(orgnr).build());

        Response response = sakRootTarget()
            .queryParam("orgnr", sak.getOrgnr())
            .request()
            .header("X-Correlation-ID", "Junit")
            .header("Authorization", authHeaderSaml)
            .get();

        verifySearchResponseMatching(response, singletonList(sak));
    }

    @Test
    void soeker_opp_saker_for_applikasjon() {
        opprett100Tilfeldigesaker();
        String applikasjon = RandomStringUtils.randomAlphabetic(9);
        Sak sak = testUtilityRepository.lagre(new SakTestData().applikasjon(applikasjon).build());

        Response response = executeGetRequest(
            sakRootTarget()
                .queryParam("applikasjon", sak.getApplikasjon())
                .queryParam("aktoerId", sak.getAktoerId()));

        verifySearchResponseMatching(response, singletonList(sak));
    }

    @Test
    void soeker_opp_saker_for_kombinasjon_av_kriterier() {
        opprett100Tilfeldigesaker();
        String fagsaknr = RandomStringUtils.randomNumeric(9);
        String applikasjon = RandomStringUtils.randomAlphabetic(9);
        String orgnr = SakTestData.generateValidOrgnr();
        String tema = RandomStringUtils.randomAlphabetic(4);

        testUtilityRepository.lagre(new SakTestData()
            .applikasjon(applikasjon)
            .orgnr(orgnr)
            .tema(tema)
            .build());

        testUtilityRepository.lagre(new SakTestData()
            .fagsakNr(fagsaknr)
            .orgnr(orgnr)
            .tema(tema)
            .build());

        testUtilityRepository.lagre(new SakTestData()
            .fagsakNr(fagsaknr)
            .applikasjon(applikasjon)
            .tema(tema)
            .build());

        testUtilityRepository.lagre(new SakTestData()
            .fagsakNr(fagsaknr)
            .applikasjon(applikasjon)
            .orgnr(orgnr)
            .build());

        Sak enesteGyldigeTreff = testUtilityRepository.lagre(new SakTestData()
            .fagsakNr(fagsaknr)
            .applikasjon(applikasjon)
            .orgnr(orgnr)
            .tema(tema)
            .build());

        Response response = executeGetRequest(
            sakRootTarget()
                .queryParam("fagsakNr", enesteGyldigeTreff.getFagsakNr())
                .queryParam("applikasjon", enesteGyldigeTreff.getApplikasjon())
                .queryParam("orgnr", enesteGyldigeTreff.getOrgnr())
                .queryParam("tema", enesteGyldigeTreff.getTema()));

        verifySearchResponseMatching(response, singletonList(enesteGyldigeTreff));
    }

    @Test
    void gir_400_naar_hverken_aktoer_orgnr_eller_faksaknr_er_angitt_i_soek() {
        opprett100Tilfeldigesaker();

        Response response = executeGetRequest(sakRootTarget());
        verify400(response);
    }

    @Test
    void soeker_opp_saker_for_flere_aktoerId() {
        opprett100Tilfeldigesaker();
        String aktoerId1 = RandomStringUtils.randomNumeric(11);
        Sak sak1 = testUtilityRepository.lagre(new SakTestData().aktoerId(aktoerId1).build());

        String aktoerId2 = RandomStringUtils.randomNumeric(11);
        Sak sak2 = testUtilityRepository.lagre(new SakTestData().aktoerId(aktoerId2).build());
        Response response = executeGetRequest(
            sakRootTarget()
                .queryParam("aktoerId", sak1.getAktoerId())
                .queryParam("aktoerId", sak2.getAktoerId()));

        verifySearchResponseMatching(response, asList(sak1, sak2));
    }

    @Override
    protected Response createSakAndTestReponse(final Sak sak) {

        final Response createdResponse =
            executePost(
                Entity
                    .json(new SakJsonTestData(sak).buildJsonString())
            );

        assertThat(createdResponse.getStatus()).isEqualTo(Response.Status.CREATED.getStatusCode());

        return createdResponse;
    }

    private void verifySearchResponseMatching(Response response, List<Sak> skalMatche) {
        skalMatche.sort(Comparator.comparingLong(Sak::getId));
        List<SakJson> sakJsons = verifySearchResponse(response, skalMatche.size());
        sakJsons.sort(Comparator.comparingLong(SakJson::getId));
        for (int i = 0; i < skalMatche.size(); i++) {
            verifyEqual(sakJsons.get(i), skalMatche.get(i));
        }
    }

    private List<SakJson> verifySearchResponse(Response response, int expectedSize) {
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(response.getHeaderString("Content-Type")).isEqualTo("application/json");
        List<SakJson> sakJsons = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (jsonElement, type, jsonDeserializationContext) -> ZonedDateTime.parse(jsonElement.getAsJsonPrimitive().getAsString()).toLocalDateTime())
            .create().fromJson(response.readEntity(String.class), new TypeToken<List<SakJson>>() {}.getType());
        assertThat(sakJsons.size()).isEqualTo(expectedSize);
        return sakJsons;
    }

    private void verify400(Response response) {
        JsonObject jsonObject = JsonParser.parseString(response.readEntity(String.class)).getAsJsonObject();
        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        assertThat(response.getHeaderString("Content-Type")).isEqualTo("application/json");
        assertThat(jsonObject.get("uuid")).isNotNull();
        assertThat(jsonObject.get("feilmelding")).isNotNull();
    }

    private void verifyEqual(SakJson sakJson, Sak sak) {
        assertThat(sakJson.getId()).isEqualTo(sak.getId());
        assertThat(sakJson.getTema()).isEqualTo(sak.getTema());
        if (StringUtils.isNotBlank(sak.getAktoerId())) {
            assertThat(sakJson.getAktoerId()).isEqualTo(sak.getAktoerId());
        } else if (StringUtils.isNotBlank(sak.getOrgnr())) {
            assertThat(sakJson.getOrgnr()).isEqualTo(sak.getOrgnr());
        }
        if (StringUtils.isNotBlank(sak.getFagsakNr())) {
            assertThat(sakJson.getFagsakNr()).isEqualTo(sak.getFagsakNr());
        }
        if(StringUtils.isNotBlank(sak.getApplikasjon())) {
            assertThat(sakJson.getApplikasjon()).isEqualTo(sak.getApplikasjon());
        }
    }

    private JsonObject createAndRetrieveAtLocation(final Sak sak) {

        final Response createdResponse = createSakAndTestReponse(sak);

        final Response getResponse =
            executeGetRequest(
                client
                    .target(createdResponse.getHeaderString("location"))
            );

        assertThat(getResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

        return JsonParser.parseString(getResponse.readEntity(String.class)).getAsJsonObject();
    }

    private void verifyBeskyttedeRessurserTilgjengelig(String header) {
        Sak sak = new SakTestData().build();
        Entity<String> json = Entity.json(
            new SakJsonTestData(sak).buildJsonString());


        Response opprettResponse = sakRootTarget()
            .request()
            .header("Authorization", header)
            .header("X-Correlation-ID", correlationId)
            .post(json);

        assertThat(opprettResponse.getStatus()).isEqualTo(Response.Status.CREATED.getStatusCode());

        Response searchResponse = sakRootTarget().queryParam("aktoerId", sak.getAktoerId())
            .request()
            .header("Authorization", header)
            .header("X-Correlation-ID", correlationId)
            .get();
        assertThat(searchResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

        Sak opprettetSak = testUtilityRepository.lagre(new SakTestData().aktoerId("123").build());

        Response getResponse = sakRootTarget().path(String.valueOf(opprettetSak.getId()))
            .request()
            .header("Authorization", header)
            .header("X-Correlation-ID", correlationId)
            .get();

        assertThat(getResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
    }
}
