package no.nav.sak;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import no.nav.sak.infrastruktur.oicd.JwtTestData;
import no.nav.sak.infrastruktur.rest.SakJson;
import no.nav.sak.repository.Sak;
import no.nav.sak.repository.SakSearchCriteria;
import no.nav.sak.repository.SakTestData;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
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
import static no.nav.sak.repository.SakTestData.choosePopulatedTema;
import static org.apache.commons.lang3.RandomStringUtils.random;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;

class SakResourceTest extends AbstractSakResourceTest {

    @Test
    void henter_sak_for_gitt_id() {
        final Sak opprettetSak =
            testUtilityRepository
                .lagre(
                    new SakTestData()
                        .aktoerId("1234567890123")
                        .build()
                );

        ResponseEntity<String> response = executeGetRequestWithBearer(URI.create(String.valueOf(opprettetSak.getSakId())), String.class);

        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(APPLICATION_JSON);

        SakJson sakJson =  new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class,
					(JsonDeserializer<LocalDateTime>) (jsonElement, _, _) ->
							ZonedDateTime.parse(jsonElement.getAsJsonPrimitive().getAsString()).toLocalDateTime())
            .create().fromJson(response.getBody(), SakJson.class);
        verifyEqual(sakJson, opprettetSak);
    }

	@Test
	void hent_sak_med_client_credentials_token() {
		final Sak opprettetSak =
				testUtilityRepository
						.lagre(
								new SakTestData()
										.aktoerId("1234567890123")
										.build()
						);

		ResponseEntity<String> response = executeGetRequest(URI.create(String.valueOf(opprettetSak.getSakId())), String.class, authHeaderBearerClientCredentialsToken);

		assertThat(response.getStatusCode()).isEqualTo(OK);
	}

	@Test
	void hent_sak_med_servicebruker_ldap() {
		final Sak opprettetSak =
				testUtilityRepository
						.lagre(
								new SakTestData()
										.aktoerId("1234567890123")
										.build()
						);

		ResponseEntity<String> response = executeGetRequest(URI.create(String.valueOf(opprettetSak.getSakId())), String.class, "Basic " + Base64.getEncoder().encodeToString("junit:password".getBytes()));

		assertThat(response.getStatusCode()).isEqualTo(OK);
	}

    @Test
    void gir_404_naar_sak_ikke_finnes_for_gitt_id() {
        ResponseEntity<String> response = executeGetRequestWithBearer(URI.create("1"), String.class);

        JsonObject jsonObject = JsonParser.parseString(response.getBody()).getAsJsonObject();
        assertThat(jsonObject.get("feilmelding").getAsString()).isNotBlank();
        assertThat(response.getStatusCode()).isEqualTo(NOT_FOUND);
    }

    @Test
    void gir_404_naar_ressurs_ikke_finnes() {
        ResponseEntity<?> response = executeGetRequestWithBearer(URI.create("/v1/finnesikke/1"));
        assertThat(response.getStatusCode()).isEqualTo(NOT_FOUND);
    }

    @Test
    void gir_405_naar_operasjon_ikke_tillatt() {
        ResponseEntity<?> response = doRequest(URI.create("1"), HttpMethod.POST, authHeaderBearerOnBehalfOfToken,
                new SakJsonTestData(new SakTestData().build()).buildJsonString(), Object.class);

        assertThat(response.getStatusCode()).isEqualTo(METHOD_NOT_ALLOWED);
    }

    @Test
    void oppretter_sak_for_aktoer() {
        Sak sak = new SakTestData()
            .aktoerId("1234567890123")
            .build();
        JsonObject jsonObject = createAndRetrieveAtLocation(sak);
        assertThat(jsonObject.get("id").getAsLong()).isNotNull();
        assertThat(jsonObject.get("aktoerId").getAsString()).isEqualTo(sak.getAktoerId());
        assertThat(jsonObject.get("orgnr").isJsonNull()).isTrue();
    }

    @Test
    void gir_400_naar_hverken_aktoer_eller_organisasjon_er_utfylt_ved_opprettelse_av_sak() {
		ResponseEntity<String> createdResponse = executePostWithBearer(new SakJsonTestData()
				.medApplikasjon(random(3))
				.medTema(random(3))
				.buildJsonString(), String.class);
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
            .aktoerId("1234567890123")
            .fagsakNr("321")
            .applikasjon("Gosys")
            .tema(choosePopulatedTema())
            .build();
        String json = (
            new SakJsonTestData(sak).buildJsonString());

        ResponseEntity<?> firstResponse = executePostWithBearer(json);

        assertThat(firstResponse.getStatusCode()).isEqualTo(CREATED);
        assertThat(firstResponse.getHeaders().getContentType()).isEqualTo(APPLICATION_JSON);

        ResponseEntity<?> secondResponse = executePostWithBearer(json);

        assertThat(secondResponse.getStatusCode()).isEqualTo(CONFLICT);
		assertThat(secondResponse.getHeaders().getContentType()).isEqualTo(APPLICATION_JSON);

        assertThat(sakJpaRepository.finnSaker(SakSearchCriteria.create())).hasSize(1);
    }

    @Test
    void gir_conflict_og_oppretter_ikke_ny_sak_dersom_tema_inaktivt() {
        Sak sak = new SakTestData()
                .aktoerId("1234567890123")
                .tema("MOB")
                .build();
        String json = (new SakJsonTestData(sak).buildJsonString());

        ResponseEntity<?> response = executePostWithBearer(json);
        assertThat(response.getStatusCode()).isEqualTo(CONFLICT);
        assertThat(response.getHeaders().getContentType()).isEqualTo(APPLICATION_JSON);
    }

    @Test
    void oppretter_ny_sak_dersom_fagsak_finnes_fra_foer_men_med_annet_tema() {
        final String AKTOER_ID = "1234567890123";
        final String FAGSAK_NR = "321";
        final String APPLIKASJON = "Gosys";
        final String TEMA_1 = "FOR";
        final String TEMA_2 = "AAP";

        Sak sak1 = new SakTestData()
                .aktoerId(AKTOER_ID)
                .fagsakNr(FAGSAK_NR)
                .applikasjon(APPLIKASJON)
                .tema(TEMA_1)
                .build();
        String jsonSak1 = (
                new SakJsonTestData(sak1).buildJsonString());

        Sak sak2 = new SakTestData()
                .aktoerId(AKTOER_ID)
                .fagsakNr(FAGSAK_NR)
                .applikasjon(APPLIKASJON)
                .tema(TEMA_2)
                .build();
        String jsonSak2 = (
                new SakJsonTestData(sak2).buildJsonString());

        ResponseEntity<?> firstResponse = executePostWithBearer(jsonSak1);

        assertThat(firstResponse.getStatusCode()).isEqualTo(CREATED);
        assertThat(firstResponse.getHeaders().getContentType()).isEqualTo(APPLICATION_JSON);

        ResponseEntity<?> secondResponse = executePostWithBearer(jsonSak2);

        assertThat(secondResponse.getStatusCode()).isEqualTo(CREATED);
        assertThat(secondResponse.getHeaders().getContentType()).isEqualTo(APPLICATION_JSON);

        assertThat(sakJpaRepository.finnSaker(SakSearchCriteria.create()))
                .hasSize(2)
                .allMatch(sak -> sak.getFagsakNr().equals(FAGSAK_NR))
                .allMatch(sak -> sak.getAktoerId().equals(AKTOER_ID))
                .allMatch(sak -> sak.getApplikasjon().equals(APPLIKASJON))
                .extracting(Sak::getTema).containsOnly(TEMA_1, TEMA_2);
    }

    @Test
    void oppretter_generell_sak_uten_applikasjon_angitt() {
        Sak sak = new SakTestData()
            .aktoerId("1234567890123")
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
            .aktoerId("1234567890123")
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
            .aktoerId("12345678901231")
            .applikasjon(null)
            .fagsakNr("123")
            .build();
        ResponseEntity<String> createdResponse = executePostWithBearer(new SakJsonTestData(sak)
            .buildJsonString(), String.class);
        verify400(createdResponse);
    }


    @Test
    void soeker_opp_saker_for_aktoer_id() {
        opprett100Tilfeldigesaker();
        String aktoerId = RandomStringUtils.secure().nextNumeric(9);
        Sak sak1 = testUtilityRepository.lagre(new SakTestData().aktoerId(aktoerId).build());
        Sak sak2 = testUtilityRepository.lagre(new SakTestData().aktoerId(aktoerId).build());

        ResponseEntity<String> response = executeGetRequestWithBearer(
				UriComponentsBuilder.newInstance()
				.queryParam("aktoerId", aktoerId)
						.build().toUri(), String.class
		);

        verifySearchResponseMatching(response, asList(sak1, sak2));
    }

    @Test
    void soeker_opp_saker_for_tema() {
        opprett100Tilfeldigesaker();
        String tema = RandomStringUtils.secure().nextAlphabetic(4);
        Sak sak = testUtilityRepository.lagre(new SakTestData().tema(tema).build());

        ResponseEntity<String> response = executeGetRequestWithBearer(
				UriComponentsBuilder.newInstance()
            .queryParam("tema", sak.getTema())
            .queryParam("aktoerId", sak.getAktoerId())
						.build().toUri(), String.class
		);

        verifySearchResponseMatching(response, singletonList(sak));
    }

    @Test
    void soeker_opp_saker_for_flere_tema() {
        opprett100Tilfeldigesaker();
        String tema1 = RandomStringUtils.secure().nextAlphabetic(4);
        String tema2 = RandomStringUtils.secure().nextAlphabetic(4);
        Sak sak1 = testUtilityRepository.lagre(new SakTestData().tema(tema1).build());
        Sak sak2 = testUtilityRepository.lagre(new SakTestData().tema(tema2)
            .aktoerId(sak1.getAktoerId())
            .build());

        ResponseEntity<String> response = executeGetRequestWithBearer(
				UriComponentsBuilder.newInstance()
				.queryParam("tema", sak1.getTema())
            .queryParam("tema", sak2.getTema())
            .queryParam("aktoerId", sak1.getAktoerId())
						.build().toUri(), String.class
		);

        verifySearchResponseMatching(response, asList(sak1, sak2));
    }

    @Test
    void soeker_opp_saker_for_fagsaknr() {
        opprett100Tilfeldigesaker();
        String fagsaknr = RandomStringUtils.secure().nextNumeric(9);
        Sak sak = testUtilityRepository.lagre(new SakTestData().
            applikasjon(RandomStringUtils.secure().nextAlphabetic(3)).
            fagsakNr(fagsaknr).build());

        ResponseEntity<String> response = executeGetRequestWithBearer(
				UriComponentsBuilder.newInstance()
            .queryParam("fagsakNr", sak.getFagsakNr())
						.build().toUri(), String.class
		);
        verifySearchResponseMatching(response, singletonList(sak));
    }

    @Test
    void soeker_opp_saker_for_orgnr() {
        opprett100Tilfeldigesaker();
        String orgnr = "974652250";
        Sak sak = testUtilityRepository.lagre(new SakTestData().orgnr(orgnr).build());

        ResponseEntity<String> response = executeGetRequestWithBearer(
				UriComponentsBuilder.newInstance()
				.queryParam("orgnr", sak.getOrgnr())
					.build().toUri(), String.class
		);

        verifySearchResponseMatching(response, singletonList(sak));
    }

    @Test
    void soeker_opp_saker_for_applikasjon() {
        opprett100Tilfeldigesaker();
        String applikasjon = RandomStringUtils.secure().nextAlphabetic(9);
        Sak sak = testUtilityRepository.lagre(new SakTestData().applikasjon(applikasjon).build());

        ResponseEntity<String> response = executeGetRequestWithBearer(
				UriComponentsBuilder.newInstance()
                .queryParam("applikasjon", sak.getApplikasjon())
                .queryParam("aktoerId", sak.getAktoerId())
					.build().toUri(), String.class
		);

        verifySearchResponseMatching(response, singletonList(sak));
    }

    @Test
    void soeker_opp_saker_for_kombinasjon_av_kriterier() {
        opprett100Tilfeldigesaker();
        String fagsaknr = RandomStringUtils.secure().nextNumeric(9);
        String applikasjon = RandomStringUtils.secure().nextAlphabetic(9);
        String orgnr = SakTestData.generateValidOrgnr();
        String tema = RandomStringUtils.secure().nextAlphabetic(4);

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

        ResponseEntity<String> response = executeGetRequestWithBearer(
				UriComponentsBuilder.newInstance()
                .queryParam("fagsakNr", enesteGyldigeTreff.getFagsakNr())
                .queryParam("applikasjon", enesteGyldigeTreff.getApplikasjon())
                .queryParam("orgnr", enesteGyldigeTreff.getOrgnr())
                .queryParam("tema", enesteGyldigeTreff.getTema())
						.build().toUri(), String.class
		);

        verifySearchResponseMatching(response, singletonList(enesteGyldigeTreff));
    }

    @Test
    void gir_400_naar_hverken_aktoer_orgnr_eller_faksaknr_er_angitt_i_soek() {
        opprett100Tilfeldigesaker();

        ResponseEntity<String> response = executeGetRequestWithBearer(null, String.class);
        verify400(response);
    }

    @Test
    void soeker_opp_saker_for_flere_aktoerId() {
        opprett100Tilfeldigesaker();
        String aktoerId1 = RandomStringUtils.secure().nextNumeric(11);
        Sak sak1 = testUtilityRepository.lagre(new SakTestData().aktoerId(aktoerId1).build());

        String aktoerId2 = RandomStringUtils.secure().nextNumeric(11);
        Sak sak2 = testUtilityRepository.lagre(new SakTestData().aktoerId(aktoerId2).build());
        ResponseEntity<String> response = executeGetRequestWithBearer(
			UriComponentsBuilder.newInstance()
                .queryParam("aktoerId", sak1.getAktoerId())
                .queryParam("aktoerId", sak2.getAktoerId())
					.build().toUri(), String.class
		);

        verifySearchResponseMatching(response, asList(sak1, sak2));
    }

    @Override
    protected ResponseEntity<Object> createSakAndTestReponse(final Sak sak) {

        final ResponseEntity<Object> createdResponse =
            executePostWithBearer(
                    new SakJsonTestData(sak).buildJsonString()
            );

        assertThat(createdResponse.getStatusCode()).isEqualTo(CREATED);

        return createdResponse;
    }

    private void verifySearchResponseMatching(ResponseEntity<String> response, List<Sak> skalMatche) {
        skalMatche.sort(Comparator.comparingLong(Sak::getSakId));
        List<SakJson> sakJsons = verifySearchResponse(response, skalMatche.size());
        sakJsons.sort(Comparator.comparingLong(SakJson::getId));
        for (int i = 0; i < skalMatche.size(); i++) {
            verifyEqual(sakJsons.get(i), skalMatche.get(i));
        }
    }

    private List<SakJson> verifySearchResponse(ResponseEntity<String> response, int expectedSize) {
        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(APPLICATION_JSON);
        List<SakJson> sakJsons = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (jsonElement, type, jsonDeserializationContext) -> ZonedDateTime.parse(jsonElement.getAsJsonPrimitive().getAsString()).toLocalDateTime())
            .create().fromJson(response.getBody(), new TypeToken<List<SakJson>>() {}.getType());
        assertThat(sakJsons.size()).isEqualTo(expectedSize);
        return sakJsons;
    }

    private void verify400(ResponseEntity<String> response) {
        JsonObject jsonObject = JsonParser.parseString(response.getBody()).getAsJsonObject();
        assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
        assertThat(response.getHeaders().getContentType()).isEqualTo(APPLICATION_JSON);
        assertThat(jsonObject.get("uuid")).isNotNull();
        assertThat(jsonObject.get("feilmelding")).isNotNull();
    }

    private void verifyEqual(SakJson sakJson, Sak sak) {
        assertThat(sakJson.getId()).isEqualTo(sak.getSakId());
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

        final ResponseEntity<Object> createdResponse = createSakAndTestReponse(sak);

        final ResponseEntity<String> getResponse =
            executeGetRequestWithBearer(
                    createdResponse.getHeaders().getLocation(), String.class
            );

        assertThat(getResponse.getStatusCode()).isEqualTo(OK);

        return JsonParser.parseString(getResponse.getBody()).getAsJsonObject();
    }

    private void verifyBeskyttedeRessurserTilgjengelig(String header) {
        Sak sak = new SakTestData().build();
        String json = new SakJsonTestData(sak).buildJsonString();


        ResponseEntity<?> opprettResponse = doRequest(null, HttpMethod.POST, header, json, Object.class);

        assertThat(opprettResponse.getStatusCode()).isEqualTo(CREATED);

		ResponseEntity<?> searchResponse = doRequest(URI.create("?aktoerId=" + sak.getAktoerId()), HttpMethod.GET, header, null, Object.class);
        assertThat(searchResponse.getStatusCode()).isEqualTo(OK);

        Sak opprettetSak = testUtilityRepository.lagre(new SakTestData().aktoerId("123").build());

		ResponseEntity<?> getResponse = doRequest(URI.create(String.valueOf(opprettetSak.getSakId())), HttpMethod.GET, header, null, Object.class);

        assertThat(getResponse.getStatusCode()).isEqualTo(OK);
    }
}
