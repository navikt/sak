package no.nav.sak;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import no.nav.sak.repository.Sak;
import no.nav.sak.repository.SakRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON;

@ActiveProfiles("itest")
@SpringBootTest(
		classes = SakTestConfiguration.class,
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
public class SakResourceExceptionHandlingTest extends AbstractSakResourceTest {

	@MockitoBean
	SakRepository sakRepository;

	@BeforeEach
	void before() {
		Mockito.doThrow(new IllegalStateException("Jeg feiler")).when(sakRepository).hentSak(Mockito.anyLong());
	}

	@Override
	protected ResponseEntity<Object> createSakAndTestReponse(Sak sak) {
		return null;
	}

	@Test
	void returnerer_500_med_uuid_og_aarsak_naar_intern_systemfeil() {
		ResponseEntity<String> response = executeGetRequestWithBearer(URI.create("1"), String.class);

		assertThat(response.getStatusCode()).isEqualTo(INTERNAL_SERVER_ERROR);
		assertThat(response.getHeaders().getContentType()).isEqualTo(APPLICATION_JSON);

		JsonObject jsonObject = JsonParser.parseString(response.getBody()).getAsJsonObject();
		assertThat(jsonObject.get("uuid")).isNotNull();
		assertThat(jsonObject.get("feilmelding")).isNotNull();
	}

}
