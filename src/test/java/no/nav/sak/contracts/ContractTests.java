package no.nav.sak.contracts;

import au.com.dius.pact.provider.junit.PactRunner;
import au.com.dius.pact.provider.junit.Provider;
import au.com.dius.pact.provider.junit.State;
import au.com.dius.pact.provider.junit.TargetRequestFilter;
import au.com.dius.pact.provider.junit.loader.PactUrl;
import au.com.dius.pact.provider.junit.target.HttpTarget;
import au.com.dius.pact.provider.junit.target.Target;
import au.com.dius.pact.provider.junit.target.TestTarget;
import no.nav.sak.Sak;
import no.nav.sak.SakConfiguration;
import no.nav.sak.SakRepository;
import no.nav.sak.infrastruktur.Database;
import no.nav.sak.infrastruktur.FlywayMigrator;
import no.nav.sak.infrastruktur.JunitDataSource;
import no.nav.sak.infrastruktur.authentication.saml.SAMLSupport;
import no.nav.sak.infrastruktur.oicd.JwtTestData;
import no.nav.sak.server.DevJetty;
import no.nav.sikkerhet.authentication.AuthenticationHeaderIdentifier;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.message.BasicHeader;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.jupiter.api.Tag;
import org.junit.runner.RunWith;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.Objects;

import static java.lang.String.valueOf;
import static no.nav.sikkerhet.authentication.AuthenticationHeaderIdentifier.SAML;
import static org.apache.commons.lang3.StringUtils.trim;

@RunWith(PactRunner.class)
@Provider("SakResource")
@PactUrl(urls = "https://raw.githubusercontent.com/navikt/pleiepenger-sak/master/pacts/pleiepenger-sak-sak.json")
@Tag("integration-test")
public class ContractTests {

    private static final int JETTY_PORT = 8101;

    private static DataSource dataSource;
    private static SakRepository sakRepository;
    private static DevJetty devJetty;

    @TestTarget
    @SuppressWarnings("unused")
    public final Target target = new HttpTarget(JETTY_PORT);


    @BeforeClass
    public static void setup() throws Exception {
        System.setProperty("sak.port", valueOf(JETTY_PORT));


        dataSource = JunitDataSource.get();
        sakRepository = new SakRepository(new Database(dataSource));
        devJetty = new DevJetty();
        devJetty.start();
    }

    @AfterClass
    public static void shutdown() throws Exception {
        devJetty.shutdown();
    }

    @Before
    @SuppressWarnings("SqlNoDataSourceInspection")
    public void resetDatabase() throws Exception {
        dataSource.getConnection().prepareStatement("DROP ALL OBJECTS").executeUpdate();
        new FlywayMigrator(dataSource).migrate();
    }

    @TargetRequestFilter
    public void addAuthenticationHeader(final HttpRequest httpRequest) {
        Header authorizationHeader = httpRequest.getFirstHeader("Authorization");
        String authIdentifier = StringUtils.substringBefore(trim(authorizationHeader.getValue()), " ");
        if(Objects.equals(AuthenticationHeaderIdentifier.OIDC.getValue(), authIdentifier)) {
            String authHeaderOIDC = "Bearer " + new JwtTestData().build();
            httpRequest.setHeader(new BasicHeader("Authorization", authHeaderOIDC));
        } else  {
            SakConfiguration sakConfiguration = new SakConfiguration();
            SAMLSupport samlSupport = new SAMLSupport(sakConfiguration);
            String samlToken = samlSupport.createNewToken();
            BasicHeader authHeaderSaml = new BasicHeader("Authorization", SAML.getValue() + " " + samlToken);
        }
    }

    @State("hentSakSomEksistererPerson")
    public void setStatehentSakSomEksistererPerson() {
        sakRepository.lagre(
            Sak.Builder.enSak()
                .medAktoerId("1000002216857")
                .medTema("BAR")
                .medApplikasjon("enApplikasjon")
                .medOpprettetAv("opprettetAvEnSaksbehandler")
                .medOpprettetTidspunkt(LocalDateTime.of(1970, 1, 1, 0, 0, 0))
                .build()
        );
    }

    @State("hentSakSomEksistererOrganisasjon")
    public void setStatehentSakSomEksistererOrganisasjon() {
        sakRepository.lagre(
            Sak.Builder.enSak()
                .medOrgnr("993554421")
                .medTema("TIL")
                .medApplikasjon("enApplikasjon")
                .medOpprettetAv("opprettetAvEnSaksbehandler")
                .medOpprettetTidspunkt(LocalDateTime.of(1970, 1, 1, 0, 0, 0))
                .build()
        );
    }
}
