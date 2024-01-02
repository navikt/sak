package no.nav.sak;

import no.nav.sak.infrastruktur.authentication.saml.SAMLSupport;
import no.nav.sak.infrastruktur.oicd.JwtClaimsTestData;
import no.nav.sak.infrastruktur.oicd.JwtTestData;
import no.nav.sak.repository.Sak;
import no.nav.sak.repository.SakTestData;
import org.joda.time.DateTime;
import org.jose4j.jwt.JwtClaims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import jakarta.annotation.Resource;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;

import static no.nav.sikkerhet.authentication.AuthenticationHeaderIdentifier.SAML;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("itest")
@SpringBootTest(
		classes = SakTestConfiguration.class,
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
public class AuthenticationFilterTest {
	private static String samlToken;
	private static SAMLSupport samlSupport;
	private Client client;
	@LocalServerPort
	private int port;

	@Resource
	SakTestTruststoreProperties sakTestTruststoreProperties;

	@BeforeEach
	void before() throws Exception {
		samlSupport = new SAMLSupport(sakTestTruststoreProperties, "123456789");
		samlToken = samlSupport.createNewToken();
		client = ClientBuilder.newClient();
	}

	@Test()
	void skal_nektes_adgang_uten_auth_header() {
		// denne feiler fordi vi kastes ut med Bad request fordi authorization-header ikke er satt
		skal_nektes_adgang_med_header("", "", Response.Status.BAD_REQUEST);
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
		skal_nektes_adgang_med_header(headerName, headerValue, Response.Status.UNAUTHORIZED);
	}

	private void skal_nektes_adgang_med_header(String headerName, String headerValue, Response.Status expectedStatus) {
		Response response = sakRootTarget().path("1").request()
				.header("X-Correlation-ID", "Junit")
				.header(headerName, headerValue).get();
		assertThat(response.getStatus()).isEqualTo(expectedStatus.getStatusCode());

		response = sakRootTarget().request()
				.header("X-Correlation-ID", "Junit")
				.header(headerName, headerValue).get();
		assertThat(response.getStatus()).isEqualTo(expectedStatus.getStatusCode());

		response = sakRootTarget().request()
				.header("X-Correlation-ID", "Junit")
				.header(headerName, headerValue).post(Entity.json(
						new SakJsonTestData(new SakTestData().build()).buildJsonString()));
		assertThat(response.getStatus()).isEqualTo(expectedStatus.getStatusCode());
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
		jwtClaims.unsetClaim("aud");
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
		return client.target("http://localhost:" + port + "/api/v1/saker");
	}

}
