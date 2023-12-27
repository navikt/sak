package no.nav.sak;

import no.nav.sak.repository.Sak;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.ResponseEntity;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;

public class SwaggerConfigurationTest extends AbstractSakResourceTest {

	@ParameterizedTest
	@CsvSource(value = {
			"/api/openapi.json/swagger-config,application/json,/api/openapi.json",
			"/api/openapi.json,application/json,Sak API",
			"/swagger-ui/index.html,text/html,Swagger UI"
	})
	void sjekk_at_swagger_er_riktig_konfigurert(String path, String contentType, String title) {
		ResponseEntity<String> response = executeGetRequestWithSaml(URI.create(path), String.class);

		assertThat(response.getStatusCode()).isEqualTo(OK);
		assertThat(response.getHeaders().getContentType().toString()).isEqualTo(contentType);
		assertThat(response.getBody()).contains(title);
	}

	@Override
	protected ResponseEntity<Object> createSakAndTestReponse(Sak sak) {
		return null;
	}
}

