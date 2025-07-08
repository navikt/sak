package no.nav.sak;

import no.nav.sak.repository.Sak;
import no.nav.sak.repository.SakJpaRepository;
import no.nav.sak.repository.SakTestData;
import no.nav.sak.repository.TestUtilityRepository;
import no.nav.security.mock.oauth2.MockOAuth2Server;
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static com.nimbusds.oauth2.sdk.token.AccessTokenType.BEARER;
import static no.nav.sak.infrastruktur.oicd.JwtTestData.entraClaims;

@ActiveProfiles("itest")
@SpringBootTest(
		classes = SakTestConfiguration.class,
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Transactional
@EnableMockOAuth2Server
public abstract class AbstractSakResourceTest {

	public static final URI SAKER_BASE_PATH = URI.create("/api/v1/saker/");
	static String authHeaderBearer;
	static String correlationId = "junit";

	@Autowired
	protected SakJpaRepository sakJpaRepository;
	@Autowired
	protected TestUtilityRepository testUtilityRepository;
	@Autowired
	TestRestTemplate testRestTemplate;
	@Autowired
	private MockOAuth2Server server;

	@BeforeEach
	void beforeAbstract() {
		authHeaderBearer = BEARER.getValue() + " " + bearerToken();
	}

	@AfterEach
	void after() {
		testUtilityRepository.resetAfterTest();
	}

	protected abstract ResponseEntity<Object> createSakAndTestReponse(final Sak sak);

	protected <T, U> ResponseEntity<T> doRequest(URI path, HttpMethod method, String authHeader, U payload, Class<T> responseType) {
		HttpHeaders headers = new HttpHeaders();
		headers.add("X-Correlation-ID", correlationId);
		headers.add("Authorization", authHeader);
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<U> httpEntity = new HttpEntity<>(payload, headers);
		URI uri = path != null ? SAKER_BASE_PATH.resolve(path) : SAKER_BASE_PATH;
		return testRestTemplate.exchange(uri, method, httpEntity, responseType);
	}

	protected ResponseEntity<Object> executeGetRequestWithBearer(URI path) {
		return executeGetRequestWithBearer(path, Object.class);
	}

	protected <T> ResponseEntity<T> executeGetRequestWithBearer(URI path, Class<T> responseType) {
		return doRequest(path, HttpMethod.GET, authHeaderBearer, null, responseType);
	}

	protected ResponseEntity<Object> executePostWithBearer(String json) {
		return executePostWithBearer(json, Object.class);
	}

	protected <T> ResponseEntity<T> executePostWithBearer(String json, Class<T> responseType) {
		return doRequest(null, HttpMethod.POST, authHeaderBearer, json, responseType);
	}

	protected void opprett100Tilfeldigesaker() {
		for (int i = 0; i < 50; i++) {
			testUtilityRepository.lagre(new SakTestData().aktoerId(RandomStringUtils.secure().nextNumeric(13)).build());
			testUtilityRepository.lagre(new SakTestData().orgnr(SakTestData.generateValidOrgnr()).build());
		}
	}

	protected String bearerToken() {
		return bearerToken(entraClaims());
	}

	private String bearerToken(Map<String, Object> claims) {
		return server.issueToken(
				"entra",
				"sak-itest",
				new DefaultOAuth2TokenCallback(
						"entra",
						"Z123456",
						"JWT",
						List.of("aud-localhost"),
						claims,
						3600
				)
		).serialize();
	}
}
