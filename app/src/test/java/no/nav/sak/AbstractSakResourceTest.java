package no.nav.sak;

import no.nav.sak.repository.Sak;
import no.nav.sak.repository.SakJpaRepository;
import no.nav.sak.repository.SakTestData;
import no.nav.sak.repository.TestUtilityRepository;
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;

import static org.springframework.http.MediaType.APPLICATION_JSON;

@ActiveProfiles("itest")
@SpringBootTest(
		classes = SakTestConfiguration.class,
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Transactional
@EnableMockOAuth2Server
public abstract class AbstractSakResourceTest extends AbstractOauth2Test{

	private final static String CORRELATION_ID = "junit";
	public static final URI SAKER_BASE_PATH = URI.create("/api/v1/saker/");
	static String authHeaderBearerOnBehalfOfToken;
	static String authHeaderBearerClientCredentialsToken;

	@Autowired
	protected SakJpaRepository sakJpaRepository;
	@Autowired
	protected TestUtilityRepository testUtilityRepository;
	@Autowired
	TestRestTemplate testRestTemplate;

	@BeforeEach
	void beforeAbstract() {
		authHeaderBearerOnBehalfOfToken = "Bearer " + oboToken();
		authHeaderBearerClientCredentialsToken = "Bearer " + clientCredentialsToken();
	}

	@AfterEach
	void after() {
		testUtilityRepository.resetAfterTest();
	}

	protected abstract ResponseEntity<Object> createSakAndTestReponse(final Sak sak);

	protected <T, U> ResponseEntity<T> doRequest(URI path, HttpMethod method, String authHeader, U payload, Class<T> responseType) {
		HttpHeaders headers = new HttpHeaders();
		headers.add("X-Correlation-ID", CORRELATION_ID);
		headers.add("Authorization", authHeader);
		headers.setContentType(APPLICATION_JSON);
		HttpEntity<U> httpEntity = new HttpEntity<>(payload, headers);
		URI uri = path != null ? SAKER_BASE_PATH.resolve(path) : SAKER_BASE_PATH;
		return testRestTemplate.exchange(uri, method, httpEntity, responseType);
	}

	protected ResponseEntity<Object> executeGetRequestWithBearer(URI path) {
		return executeGetRequestWithBearer(path, Object.class);
	}

	protected <T> ResponseEntity<T> executeGetRequest(URI path, Class<T> responseType, String authHeader) {
		return doRequest(path, HttpMethod.GET, authHeader, null, responseType);
	}

	protected <T> ResponseEntity<T> executeGetRequestWithBearer(URI path, Class<T> responseType) {
		return doRequest(path, HttpMethod.GET, authHeaderBearerOnBehalfOfToken, null, responseType);
	}

	protected ResponseEntity<Object> executePostWithBearer(String json) {
		return executePostWithBearer(json, Object.class);
	}

	protected <T> ResponseEntity<T> executePostWithBearer(String json, Class<T> responseType) {
		return doRequest(null, HttpMethod.POST, authHeaderBearerOnBehalfOfToken, json, responseType);
	}

	protected void opprett100Tilfeldigesaker() {
		for (int i = 0; i < 50; i++) {
			testUtilityRepository.lagre(new SakTestData().aktoerId(RandomStringUtils.secure().nextNumeric(13)).build());
			testUtilityRepository.lagre(new SakTestData().orgnr(SakTestData.generateValidOrgnr()).build());
		}
	}
}
