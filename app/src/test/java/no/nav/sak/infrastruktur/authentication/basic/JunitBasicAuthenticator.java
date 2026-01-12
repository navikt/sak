package no.nav.sak.infrastruktur.authentication.basic;

import no.nav.sak.configuration.SakProperties;
import no.nav.sak.infrastruktur.authentication.AuthenticationResult;
import no.nav.sak.infrastruktur.authentication.LdapConfiguration;
import no.nav.sak.infrastruktur.authentication.BasicAuthenticator;
import org.ehcache.Cache;

import java.util.Objects;

import static org.mockito.Mockito.mock;

@SuppressWarnings("unchecked")
public class JunitBasicAuthenticator extends BasicAuthenticator {
    public static final String USERNAME = "junit";
    public static final String PASSWORD = "password";

    public JunitBasicAuthenticator(SakProperties sakProperties, LdapConfiguration ldapConfiguration) {
        super(sakProperties, ldapConfiguration, mock(Cache.class));
    }

    public AuthenticationResult authenticateWithLdap(String username, String password) {
        if (Objects.equals(username, USERNAME) && Objects.equals(password, PASSWORD)) {
            return AuthenticationResult.success(username, username);
        } else {
            return AuthenticationResult.invalid("Ikke en gyldig kombinasjon for brukernavn/passord i unit-test");
        }
    }
}
