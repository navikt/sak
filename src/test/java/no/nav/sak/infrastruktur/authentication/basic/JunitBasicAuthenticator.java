package no.nav.sak.infrastruktur.authentication.basic;

import no.nav.sikkerhet.authentication.AuthenticationResult;
import no.nav.sikkerhet.authentication.basic.BasicAuthenticator;
import no.nav.sikkerhet.authentication.basic.LdapConfiguration;
import org.ehcache.Cache;

import java.util.Objects;

import static org.mockito.Mockito.mock;

@SuppressWarnings("unchecked")
public class JunitBasicAuthenticator extends BasicAuthenticator {
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";

    public JunitBasicAuthenticator(LdapConfiguration ldapConfiguration) {
        super(ldapConfiguration, mock(Cache.class));
    }

    public AuthenticationResult authenticateWithLdap(String username, String password) {
        if (Objects.equals(username, USERNAME) && Objects.equals(password, PASSWORD)) {
            return AuthenticationResult.success(username, username);
        } else {
            return AuthenticationResult.invalid("Ikke en gyldig komibinasjon for brukernavn/passord i unit-test");
        }
    }
}
