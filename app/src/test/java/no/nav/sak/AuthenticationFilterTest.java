package no.nav.sak;

import jakarta.annotation.Resource;
import jakarta.ws.rs.client.Entity;
import no.nav.sak.infrastruktur.authentication.saml.SAMLSupport;
import no.nav.sak.infrastruktur.oicd.JwtClaimsTestData;
import no.nav.sak.infrastruktur.oicd.JwtTestData;
import no.nav.sak.repository.Sak;
import no.nav.sak.repository.SakTestData;
import org.jose4j.jwt.JwtClaims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;

import static java.time.temporal.ChronoUnit.HOURS;
import static no.nav.sak.infrastruktur.authentication.AuthenticationHeaderIdentifier.SAML;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@ActiveProfiles("itest")
@SpringBootTest(
		classes = SakTestConfiguration.class,
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
public class AuthenticationFilterTest {
	private static String samlToken;
	private static SAMLSupport samlSupport;
	@Resource
	TestRestTemplate testRestTemplate;

	@Resource
	SakTestTruststoreProperties sakTestTruststoreProperties;
	@Resource
	Clock clock;

	@BeforeEach
	void before() {
		samlSupport = new SAMLSupport(sakTestTruststoreProperties, "123456789", clock);
		samlToken = samlSupport.createNewToken();
	}

	@Test()
	void skal_nektes_adgang_uten_auth_header() {
		skal_nektes_adgang_med_header(null, "", UNAUTHORIZED);
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
		skal_nektes_adgang_med_header(headerName, headerValue, UNAUTHORIZED);
	}

	private void skal_nektes_adgang_med_header(String headerName, String headerValue, HttpStatus expectedStatus) {
		HttpHeaders headers = new HttpHeaders();
		headers.add("X-Correlation-ID", "Junit");
		if (headerName != null) {
			headers.add(headerName, headerValue);
		}

		HttpEntity<String> httpEntity1 = new HttpEntity<>(headers);
		ResponseEntity<String> response1 = testRestTemplate.exchange("/api/v1/saker/" + "1", HttpMethod.GET, httpEntity1, String.class);
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
	void skal_nektes_adgang_naar_token_enda_ikke_gyldig() {
		String token = samlSupport.createNewToken(clock.instant().plus(24, HOURS), clock.instant().plus(25, HOURS));

		String header = SAML.getValue() + " " + token;
		ResponseEntity<String> response = doRequest(HttpMethod.GET, header, String.class);
		assertThat(response.getStatusCode()	).isEqualTo(UNAUTHORIZED);
	}

	@Test
	void skal_nektes_adgang_naar_token_er_expired() {
		String token = samlSupport.createNewToken(clock.instant().minus(2*24, HOURS), clock.instant().minus(24, HOURS));

		String header = SAML.getValue() + " " + token;
		ResponseEntity<String> response = doRequest(HttpMethod.GET, header, String.class);
		assertThat(response.getStatusCode()	).isEqualTo(UNAUTHORIZED);
	}

	@Test
	void beskyttede_ressurser_ikke_tilgjengelige_naar_ugyldig_authheader_for_oidc() throws Exception {
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

	private <T,U> ResponseEntity<T> doRequest(String path, HttpMethod method, String authHeader, U payload, Class<T> responseType) {
		HttpHeaders headers = new HttpHeaders();
		headers.add("X-Correlation-ID", "Junit");
		headers.add("Authorization", authHeader);
		HttpEntity<U> httpEntity = new HttpEntity<>(payload, headers);
		return testRestTemplate.exchange("/api/v1/saker/" + path, method, httpEntity, responseType);
	}

}
