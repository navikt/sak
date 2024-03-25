package no.nav.sak;

import no.nav.sak.repository.Sak;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;

public class UnprotectedResourcesConfigurationTest extends AbstractSakResourceTest {

	@ParameterizedTest
	@CsvSource(value = {
			"/actuator/prometheus,text/plain,jvm_memory_max_bytes",
			"/v3/api-docs/swagger-config,application/json,/v3/api-docs",
			"/v3/api-docs,application/json,Sak API",
			"/swagger-ui/index.html,text/html,Swagger UI"
	})
	void sjekk_at_swagger_og_actuator_er_tilgjengelig_uten_begrensninger(String path, String contentType, String title) {
		HttpEntity<Object> httpEntity = new HttpEntity<>(null, new HttpHeaders());
		URI uri = SAKER_BASE_PATH.resolve(URI.create(path));
		ResponseEntity<String> response = testRestTemplate.exchange(uri, HttpMethod.GET, httpEntity, String.class);

		assertThat(response.getStatusCode()).isEqualTo(OK);
		assertThat(response.getHeaders().getContentType().toString()).startsWith(contentType);
		assertThat(response.getBody()).contains(title);
	}

	@Override
	protected ResponseEntity<Object> createSakAndTestReponse(Sak sak) {
		return null;
	}
}

