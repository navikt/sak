package no.nav.sak.infrastruktur.authentication.basic;

import no.nav.sikkerhet.authentication.AuthenticationResult;
import no.nav.sikkerhet.authentication.basic.BasicAuthenticator;
import no.nav.sikkerhet.authentication.basic.LdapConfiguration;

import java.util.Objects;

public class JunitBasicAuthenticator extends BasicAuthenticator {
    public static final String USERNAME = "username";
    public static final String ***passord=gammelt_passord***";

    public JunitBasicAuthenticator(LdapConfiguration ldapConfiguration) {
        super(ldapConfiguration);
    }

    public AuthenticationResult authenticateWithLdap(String username, String password) {
        if (Objects.equals(username, USERNAME) && Objects.equals(password, PASSWORD)) {
            return AuthenticationResult.success(username, username);
        } else {
            return AuthenticationResult.invalid("Ikke en gyldig komibinasjon for brukernavn/passord i unit-test");
        }
    }
}
