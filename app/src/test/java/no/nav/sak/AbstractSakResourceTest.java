package no.nav.sak;

import no.nav.sak.infrastruktur.authentication.saml.SAMLSupport;
import no.nav.sak.repository.Sak;
import no.nav.sak.repository.SakRepository;
import no.nav.sak.repository.SakTestData;
import no.nav.sak.repository.TestUtilityRepository;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import jakarta.annotation.Resource;

import java.net.URI;
import java.time.Clock;

import static no.nav.sak.infrastruktur.authentication.AuthenticationHeaderIdentifier.SAML;

@ActiveProfiles("itest")
@SpringBootTest(
		classes = SakTestConfiguration.class,
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
public abstract class AbstractSakResourceTest {

	public static final URI SAKER_BASE_PATH = URI.create("/api/v1/saker/");
	static String authHeaderSaml;
	static String correlationId = "junit";

	@Resource
	SakRepository sakRepository;
	@Resource
	protected TestUtilityRepository testUtilityRepository;
	@Resource
	SakTestTruststoreProperties sakTestTruststoreProperties;
	@Resource
	TestRestTemplate testRestTemplate;
	@Resource
	Clock clock;

	@BeforeEach
	void before() {
		SAMLSupport samlSupport = new SAMLSupport(sakTestTruststoreProperties, "123456789", clock);
		String samlToken = samlSupport.createNewToken();
		authHeaderSaml = SAML.getValue() + " " + samlToken;
	}

	@AfterEach
	void after() {
		testUtilityRepository.resetAfterTest();
	}

	protected abstract ResponseEntity<Object> createSakAndTestReponse(final Sak sak);

	protected  <T,U> ResponseEntity<T> doRequest(URI path, HttpMethod method, String authHeader, U payload, Class<T> responseType) {
		HttpHeaders headers = new HttpHeaders();
		headers.add("X-Correlation-ID", correlationId);
		headers.add("Authorization", authHeader);
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<U> httpEntity = new HttpEntity<>(payload, headers);
		URI uri = path != null ? SAKER_BASE_PATH.resolve(path) : SAKER_BASE_PATH;
		return testRestTemplate.exchange(uri, method, httpEntity, responseType);
	}

	protected ResponseEntity<Object> executeGetRequestWithSaml(URI path) {
		return executeGetRequestWithSaml(path, Object.class);
	}

	protected <T> ResponseEntity<T> executeGetRequestWithSaml(URI path, Class<T> responseType) {
		return doRequest(path, HttpMethod.GET, authHeaderSaml, null, responseType);
	}

	protected ResponseEntity<Object> executePostWithSaml(String json) {
		return executePostWithSaml(json, Object.class);
	}

	protected <T> ResponseEntity<T> executePostWithSaml(String json, Class<T> responseType) {
		return doRequest(null, HttpMethod.POST, authHeaderSaml, json, responseType);
	}

	protected void opprett100Tilfeldigesaker() {
		for (int i = 0; i < 50; i++) {
			testUtilityRepository.lagre(new SakTestData().aktoerId(RandomStringUtils.randomNumeric(5)).build());
			testUtilityRepository.lagre(new SakTestData().orgnr(SakTestData.generateValidOrgnr()).build());
		}
	}
}
