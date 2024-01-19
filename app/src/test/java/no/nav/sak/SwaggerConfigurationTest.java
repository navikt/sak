package no.nav.sak;

import jakarta.ws.rs.core.Response;
import no.nav.sak.repository.Sak;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

public class SwaggerConfigurationTest extends AbstractSakResourceTest {

	@ParameterizedTest
	@CsvSource(value = {
			"/api/openapi.json/swagger-config,application/json,/api/openapi.json",
			"/api/openapi.json,application/json,Sak API",
			"/swagger-ui/index.html,text/html,Swagger UI"
	})
	void sjekk_at_swagger_er_riktig_konfigurert(String path, String contentType, String title) {
		Response response = executeGetRequest(client.target("http://localhost:" + port).path(path));

		assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
		assertThat(response.getHeaderString("Content-Type")).isEqualTo(contentType);
		assertThat(response.readEntity(String.class)).contains(title);
	}

	@Override
	protected Response createSakAndTestReponse(Sak sak) {
		return null;
	}
}

