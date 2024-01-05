package no.nav.sak.infrastruktur.authentication;

import org.apache.commons.lang3.StringUtils;

import static no.nav.sak.infrastruktur.authentication.AuthenticationHeaderIdentifier.BASIC;
import static no.nav.sak.infrastruktur.authentication.AuthenticationHeaderIdentifier.OIDC;
import static no.nav.sak.infrastruktur.authentication.AuthenticationHeaderIdentifier.SAML;


public class Authenticator {
	private final SAMLValidator samlValidator;
	private final BasicAuthenticator basicAuthenticator;
	private final OidcTokenValidator oidcTokenValidator;

	public Authenticator(OidcTokenValidator oidcTokenValidator, SAMLValidator samlValidator, BasicAuthenticator basicAuthenticator) {
		this.oidcTokenValidator = oidcTokenValidator;
		this.samlValidator = samlValidator;
		this.basicAuthenticator = basicAuthenticator;
	}

	public AuthenticationResult authenticate(String authHeader) {
		if (StringUtils.isNotEmpty(authHeader)) {
			String[] authorizationHeader = authHeader.trim().split(" ");
			if (authorizationHeader.length != 2) {
				return AuthenticationResult.invalid("Malformed Authorization header");
			}

			String identifier = authorizationHeader[0];
			String credentials = authorizationHeader[1];

			if (OIDC.getValue().equals(identifier)) {
				return oidcTokenValidator.validate(credentials);
			} else if (SAML.getValue().equals(identifier)) {
				return samlValidator.validate(credentials);
			} else if (BASIC.getValue().equals(identifier)) {
				return basicAuthenticator.authenticate(credentials);
			} else {
				return AuthenticationResult.invalid("Authorization header identifier not supported");
			}
		} else {
			return AuthenticationResult.invalid("Authorization header missing");
		}
	}
}
