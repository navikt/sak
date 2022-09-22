package no.nav.sak;

import no.nav.resilience.ResilienceConfig;
import no.nav.sak.infrastruktur.abac.SakPEP;
import no.nav.sak.infrastruktur.authentication.AuthenticationFilter;
import no.nav.sak.infrastruktur.authentication.basic.JunitBasicAuthenticator;
import no.nav.sak.infrastruktur.oicd.JunitJsonWebKey;
import no.nav.sak.infrastruktur.oicd.JwtClaimsTestData;
import no.nav.sak.repository.Database;
import no.nav.sak.repository.FlywayMigrator;
import no.nav.sak.repository.JunitDatabase;
import no.nav.sak.repository.SakRepository;
import no.nav.sikkerhet.abac.ABACClient;
import no.nav.sikkerhet.authentication.Authenticator;
import no.nav.sikkerhet.authentication.basic.LdapConfiguration;
import no.nav.sikkerhet.authentication.oidc.OidcTokenValidator;
import no.nav.sikkerhet.authentication.saml.SAMLValidator;
import org.jose4j.keys.resolvers.JwksVerificationKeyResolver;
import org.jose4j.keys.resolvers.VerificationKeyResolver;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.singletonList;

abstract class AbstractSakJunitApplication extends SakApplication {
    AbstractSakJunitApplication() {
        super(JunitDatabase.get());
    }

    @Override
    protected Database createDatabase(DataSource dataSource) {
        return JunitDatabase.get();
    }

    @Override
    void registerAuthenticationFilter(SakConfiguration sakConfiguration) {
        Map<String, VerificationKeyResolver> verificationKeyResolverMap = new HashMap<>();
        verificationKeyResolverMap.put(JwtClaimsTestData.ISSUER, new JwksVerificationKeyResolver(
            singletonList(JunitJsonWebKey.get())));
        OidcTokenValidator oidcTokenValidator = new OidcTokenValidator(verificationKeyResolverMap);

        SAMLValidator samlValidator = new SAMLValidator(
            sakConfiguration.getRequiredString("sak.junit-truststore.path"),
            sakConfiguration.getRequiredString("sak.junit-truststore.password"));

        LdapConfiguration ldapConfiguration = LdapConfiguration.builder()
                .withUrl(sakConfiguration.getRequiredString("LDAP_URL"))
                .withServiceUserBaseDN(sakConfiguration.getRequiredString("LDAP_SERVICEUSER_BASEDN"))
                .withBindUser(sakConfiguration.getRequiredString("LDAP_USERNAME"))
                .withBindPassword(null)
                .build();

        JunitBasicAuthenticator junitBasicAuthenticator = new JunitBasicAuthenticator(ldapConfiguration);

        Authenticator authenticator = new Authenticator(oidcTokenValidator, samlValidator, junitBasicAuthenticator);
        register(new AuthenticationFilter(authenticator, ResilienceConfig.ofDefaults()));
    }

    @Override
    void migrateSak(DataSource dataSource) {
        new FlywayMigrator(dataSource, "classpath:db/migration", "classpath:db/h2/migration").migrate();
    }

    @Override
    void registerApiResources(Database database, SakConfiguration sakConfiguration) {
        register(new SakResource(
            new SakRepository(database),
            new SakPEP(createAbacClient(sakConfiguration),ResilienceConfig.ofDefaults()))
        );
    }

    void migrateDataWarehouse(SakConfiguration sakConfiguration){}

    protected abstract ABACClient createAbacClient(SakConfiguration sakConfiguration);
}
