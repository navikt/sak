package no.nav.sak;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import no.nav.sak.infrastruktur.authentication.saml.SAMLSupport;
import no.nav.sak.repository.SakRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import jakarta.annotation.Resource;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;

import static no.nav.sikkerhet.authentication.AuthenticationHeaderIdentifier.SAML;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("itest")
@SpringBootTest(
		classes = SakTestConfiguration.class,
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
public class SakResourceExceptionHandlingTest {

	private String samlToken;
	private Client client;
	@LocalServerPort
	private int port;
	@Resource
	SakTestTruststoreProperties sakTestTruststoreProperties;

	@MockBean
	SakRepository sakRepository;

	@BeforeEach
	void before() {
		client = ClientBuilder.newClient();
		SAMLSupport samlSupport = new SAMLSupport(sakTestTruststoreProperties, "123456789");
		samlToken = samlSupport.createNewToken();
		Mockito.doThrow(new IllegalStateException("Jeg feiler")).when(sakRepository).hentSak(Mockito.anyLong());
	}

	@Test
	void returnerer_500_med_uuid_og_aarsak_naar_intern_systemfeil() {
		String samlHeader = SAML.getValue() + " " + samlToken;

		Response response = sakRootTarget().path("1").request()
				.header("Authorization", samlHeader)
				.header("X-Correlation-Id", "jUnit")
				.get();

		assertThat(response.getStatus()).isEqualTo(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
		assertThat(response.getHeaderString("Content-Type")).isEqualTo("application/json");

		JsonObject jsonObject = JsonParser.parseString(response.readEntity(String.class)).getAsJsonObject();
		assertThat(jsonObject.get("uuid")).isNotNull();
		assertThat(jsonObject.get("feilmelding")).isNotNull();
	}

	private WebTarget sakRootTarget() {
		return client.target("http://localhost:" + port + "/api/v1/saker");
	}
}
