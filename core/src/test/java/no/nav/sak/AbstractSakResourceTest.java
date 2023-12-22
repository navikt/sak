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
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import javax.annotation.Resource;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import static no.nav.sikkerhet.authentication.AuthenticationHeaderIdentifier.SAML;

@ActiveProfiles("itest")
@SpringBootTest(
		classes = SakTestConfiguration.class,
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
public abstract class AbstractSakResourceTest {

	static String authHeaderSaml;
	static String correlationId = "junit";

	protected Client client;
	@LocalServerPort
	protected int port;

	@Resource
	SakRepository sakRepository;
	@Resource
	protected TestUtilityRepository testUtilityRepository;
	@Resource
	SakTestTruststoreProperties sakTestTruststoreProperties;

	@BeforeEach
	void before() {
		SAMLSupport samlSupport = new SAMLSupport(sakTestTruststoreProperties, "123456789");
		String samlToken = samlSupport.createNewToken();
		authHeaderSaml = SAML.getValue() + " " + samlToken;
		client = ClientBuilder.newClient();
	}

	@AfterEach
	void after() {
		testUtilityRepository.resetAfterTest();
	}

	protected abstract Response createSakAndTestReponse(final Sak sak);

	protected Response executeGetRequest(WebTarget target) {
		return target.request()
				.header("Authorization", authHeaderSaml)
				.header("X-Correlation-ID", correlationId)
				.get();
	}

	protected WebTarget sakRootTarget() {
		return client.target("http://localhost:" + port + "/api/v1/saker");
	}

	protected void opprett100Tilfeldigesaker() {
		for (int i = 0; i < 50; i++) {
			testUtilityRepository.lagre(new SakTestData().aktoerId(RandomStringUtils.randomNumeric(5)).build());
			testUtilityRepository.lagre(new SakTestData().orgnr(SakTestData.generateValidOrgnr()).build());
		}
	}

	protected Response executePost(Entity<String> json) {
		return sakRootTarget()
				.request()
				.header("Authorization", authHeaderSaml)
				.header("X-Correlation-ID", correlationId)
				.post(json);
	}
}
