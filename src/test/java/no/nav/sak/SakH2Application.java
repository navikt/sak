package no.nav.sak;

import no.nav.sak.infrastruktur.FlywayMigrator;
import no.nav.sak.infrastruktur.JunitDataSource;
import no.nav.sak.infrastruktur.abac.ABACJunitClient;
import no.nav.sak.infrastruktur.authentication.AuthenticationFilter;
import no.nav.sak.infrastruktur.authentication.basic.JunitBasicAuthenticator;
import no.nav.sak.infrastruktur.oicd.JunitJsonWebKey;
import no.nav.sak.infrastruktur.oicd.JwtClaimsTestData;
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

public class SakH2Application extends SakApplication {
    protected DataSource createSakDataSource(SakConfiguration sakConfiguration) {
        return JunitDataSource.get();
    }

    void migrateDataWarehouse(SakConfiguration sakConfiguration){}

    void migrateSak(DataSource dataSource) {
        new FlywayMigrator(dataSource, "classpath:db/migration", "classpath:/db/h2/migration").migrate();
    }


    protected ABACClient createAbacClient(SakConfiguration sakConfiguration) {
        return ABACJunitClient.create();
    }

    void registerAuthenticationFilter(SakConfiguration sakConfiguration) {
        Map<String, VerificationKeyResolver> verificationKeyResolverMap = new HashMap<>();
        verificationKeyResolverMap.put(JwtClaimsTestData.ISSUER, new JwksVerificationKeyResolver(
            singletonList(JunitJsonWebKey.get())));
        OidcTokenValidator oidcTokenValidator = new OidcTokenValidator(verificationKeyResolverMap);

        SAMLValidator samlValidator = new SAMLValidator(
            sakConfiguration.getRequiredString("sak.junit-truststore.path"),
            sakConfiguration.getRequiredString("sak.junit-truststore.password"));

        LdapConfiguration ldapConfiguration = new LdapConfiguration(
            sakConfiguration.getRequiredString("LDAP_SERVICEUSER_BASEDN"),
            sakConfiguration.getRequiredString("LDAP_URL"),
            sakConfiguration.getRequiredString("LDAP_USERNAME"),
            null);

        JunitBasicAuthenticator junitBasicAuthenticator = new JunitBasicAuthenticator(ldapConfiguration);

        Authenticator authenticator = new Authenticator(oidcTokenValidator, samlValidator, junitBasicAuthenticator);
        register(new AuthenticationFilter(authenticator));
    }

}
