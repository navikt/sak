package no.nav.sak;

import jakarta.ws.rs.client.Entity;
import no.nav.sak.infrastruktur.oicd.JwtClaimsTestData;
import no.nav.sak.infrastruktur.oicd.JwtTestData;
import no.nav.sak.repository.Sak;
import no.nav.sak.repository.SakTestData;
import org.jose4j.jwt.JwtClaims;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static com.nimbusds.oauth2.sdk.token.AccessTokenType.BEARER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@ActiveProfiles("itest")
@SpringBootTest(
		classes = SakTestConfiguration.class,
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
public class AuthenticationFilterTest extends AbstractOauth2Test {
	@Autowired
	TestRestTemplate testRestTemplate;

	@Test
	void skal_gi_tilgang_for_obo_token() {
		skal_gis_tilgang_med_token(oboToken());
	}

	@Test
	void skal_gi_tilgang_for_client_credentials_token() {
		skal_gis_tilgang_med_token(clientCredentialsToken());
	}

	@Test
	void skal_gi_tilgang_for_sts_token() {
		skal_gis_tilgang_med_token(stsToken());
	}

	@Test
	void skal_gi_tilgang_til_actuator_uten_token() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("X-Correlation-ID", "Junit");
		HttpEntity<String> httpEntity = new HttpEntity<>(headers);
		ResponseEntity<String> responseEntity = testRestTemplate.exchange("/actuator/health", HttpMethod.GET, httpEntity, String.class);
		assertThat(responseEntity.getStatusCode()).isEqualTo(OK);
	}

	private void skal_gis_tilgang_med_token(String token) {
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(token);
		headers.add("X-Correlation-ID", "Junit");
		HttpEntity<String> httpEntity = new HttpEntity<>(headers);
		ResponseEntity<String> responseEntity = testRestTemplate.exchange("/api/v1/saker/", HttpMethod.GET, httpEntity, String.class);
		assertThat(responseEntity.getStatusCode()).isNotEqualTo(UNAUTHORIZED);
	}

	@Test()
	void skal_nektes_adgang_uten_auth_header() {
		skal_nektes_adgang_med_header(null, "", UNAUTHORIZED);
	}

	@Test
	void skal_nektes_adgang_uten_gyldig_bearer_token() {
		skal_nektes_adgang_med_header("Authorization", "Bearer Invalidtoken");
	}

	@Test
	void skal_nektes_adgang_uten_gyldig_header() {
		skal_nektes_adgang_med_header("Auth", oboToken());
	}

	@Test
	void skal_nektes_adgang_uten_gyldig_auth_header_identifier() {
		skal_nektes_adgang_med_header("Authorization", "Invalididentifier" + " " + oboToken());
	}

	private void skal_nektes_adgang_med_header(String headerName, String headerValue) {
		skal_nektes_adgang_med_header(headerName, headerValue, UNAUTHORIZED);
	}

	private void skal_nektes_adgang_med_header(String headerName, String headerValue, HttpStatus expectedStatus) {
		HttpHeaders headers = new HttpHeaders();
		headers.add("X-Correlation-ID", "Junit");
		if (headerName != null) {
			headers.add(headerName, headerValue);
		}

		HttpEntity<String> httpEntity1 = new HttpEntity<>(headers);
		ResponseEntity<String> response1 = testRestTemplate.exchange("/api/v1/saker/1", HttpMethod.GET, httpEntity1, String.class);
		assertThat(response1.getStatusCode()).isEqualTo(expectedStatus);

		HttpEntity<String> httpEntity2 = new HttpEntity<>(headers);
		ResponseEntity<String> response2 = testRestTemplate.exchange("/api/v1/saker/", HttpMethod.GET, httpEntity2, String.class);
		assertThat(response2.getStatusCode()).isEqualTo(expectedStatus);

		String payload = new SakJsonTestData(new SakTestData().build()).buildJsonString();
		HttpEntity<String> httpEntity3 = new HttpEntity<>(payload, headers);
		ResponseEntity<String> response3 = testRestTemplate.exchange("/api/v1/saker/", HttpMethod.POST, httpEntity3, String.class);
		assertThat(response3.getStatusCode()).isEqualTo(expectedStatus);
	}

	@Test
	void skal_nektes_adgang_naar_token_er_expired() {
		String header = BEARER.getValue() + " " + expiredOboToken();

		ResponseEntity<String> response = doRequest(HttpMethod.GET, header, String.class);
		assertThat(response.getStatusCode()).isEqualTo(UNAUTHORIZED);
	}

	@Test
	void beskyttede_ressurser_ikke_tilgjengelige_naar_ugyldig_authheader_for_oidc() {
		Sak sak = new SakTestData().build();
		Entity<String> json = Entity.json(
				new SakJsonTestData(sak).buildJsonString());
		JwtClaims jwtClaims = new JwtClaimsTestData().build();
		jwtClaims.unsetClaim("aud");
		String authHeader = "Bearer " + new JwtTestData()
				.claims(jwtClaims)
				.build();

		ResponseEntity<String> opprettResponse = doRequest("", HttpMethod.POST, authHeader, json, String.class);
		assertThat(opprettResponse.getStatusCode()).isEqualTo(UNAUTHORIZED);

		ResponseEntity<String> searchResponse = doRequest(HttpMethod.GET, authHeader, String.class);
		assertThat(searchResponse.getStatusCode()).isEqualTo(UNAUTHORIZED);

		ResponseEntity<String> getResponse = doRequest("1", HttpMethod.GET, authHeader, null, String.class);
		assertThat(getResponse.getStatusCode()).isEqualTo(UNAUTHORIZED);
	}

	private <T> ResponseEntity<T> doRequest(HttpMethod method, String authHeader, Class<T> responseType) {
		return doRequest("", method, authHeader, null, responseType);
	}

	private <T, U> ResponseEntity<T> doRequest(String path, HttpMethod method, String authHeader, U payload, Class<T> responseType) {
		HttpHeaders headers = new HttpHeaders();
		headers.add("X-Correlation-ID", "Junit");
		headers.add("Authorization", authHeader);
		HttpEntity<U> httpEntity = new HttpEntity<>(payload, headers);
		return testRestTemplate.exchange("/api/v1/saker/" + path, method, httpEntity, responseType);
	}
}
