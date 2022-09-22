package no.nav.sak;

import no.nav.sak.SakConfiguration;
import no.nav.sak.infrastruktur.JunitTransactionSupport;
import no.nav.sak.infrastruktur.authentication.saml.SAMLSupport;
import no.nav.sak.repository.Database;
import no.nav.sak.repository.JunitDatabase;
import no.nav.sak.repository.Sak;
import no.nav.sak.repository.SakRepository;
import no.nav.sak.repository.SakTestData;
import org.apache.commons.lang3.RandomStringUtils;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import static no.nav.sikkerhet.authentication.AuthenticationHeaderIdentifier.SAML;

public abstract class AbstractSakResourceTest extends JerseyTest {

    private static final Database database = JunitDatabase.get();

    public static final SakRepository sakRepository = new SakRepository(database);
    static String authHeaderSaml;
    static String correlationId = "junit";

    private final JunitTransactionSupport junitTransactionSupport = new JunitTransactionSupport(database);

    @BeforeAll
    static void setup() {
        SakConfiguration sakConfiguration = new SakConfiguration();
        SAMLSupport samlSupport = new SAMLSupport(sakConfiguration);
        String samlToken = samlSupport.createNewToken();
        authHeaderSaml = SAML.getValue() + " " + samlToken;
    }

    @BeforeEach
    void before() throws Exception {
        super.setUp();
        junitTransactionSupport.initTransaction();
    }

    @AfterEach
    void after() throws Exception {
        super.tearDown();
        junitTransactionSupport.rollback();
    }

    protected abstract Response createSakAndTestReponse(final Sak sak);

    protected Response executeGetRequest(WebTarget target) {
        return target.request()
            .header("Authorization", authHeaderSaml)
            .header("X-Correlation-ID", correlationId)
            .get();
    }

    protected WebTarget sakRootTarget() {
        return target("/v1/saker");
    }

    protected void opprett100Tilfeldigesaker() {
        for (int i = 0; i < 50; i++) {
            sakRepository.lagre(new SakTestData().aktoerId(RandomStringUtils.randomNumeric(5)).build());
            sakRepository.lagre(new SakTestData().orgnr(SakTestData.generateValidOrgnr()).build());
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
