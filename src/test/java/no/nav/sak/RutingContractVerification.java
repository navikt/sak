package no.nav.sak;

import au.com.dius.pact.provider.junit.PactRunner;
import au.com.dius.pact.provider.junit.Provider;
import au.com.dius.pact.provider.junit.State;
import au.com.dius.pact.provider.junit.TargetRequestFilter;
import au.com.dius.pact.provider.junit.loader.PactFolder;
import au.com.dius.pact.provider.junit.target.HttpTarget;
import au.com.dius.pact.provider.junit.target.Target;
import au.com.dius.pact.provider.junit.target.TestTarget;
import no.nav.sak.infrastruktur.Database;
import no.nav.sak.infrastruktur.FlywayMigrator;
import no.nav.sak.infrastruktur.JunitDataSource;
import no.nav.sak.infrastruktur.sts.STSSupport;
import no.nav.sak.server.DevJetty;
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

import static java.lang.String.valueOf;
import static no.nav.sikkerhet.authentication.AuthenticationHeaderIdentifier.SAML;

@RunWith(PactRunner.class)
@Provider("SakResource")
@PactFolder("pacts")
@Tag("integration-test")
public class RutingContractVerification {

    private static final int JETTY_PORT = 8101;
    private static Header authHeaderSaml;
    private static DataSource dataSource;
    private static SakRepository sakRepository;
    private static DevJetty devJetty;

    @TestTarget
    @SuppressWarnings("unused")
    public final Target target = new HttpTarget(JETTY_PORT);


    @BeforeClass
    public static void setup() throws Exception {
        System.setProperty("sak.port", valueOf(JETTY_PORT));
        authHeaderSaml = new BasicHeader("Authorization", SAML.getValue() + " " + new STSSupport().getSystemSAMLTokenFromSTS());
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
        httpRequest.setHeader(authHeaderSaml);
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
