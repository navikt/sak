package no.nav.sak.infrastruktur.authentication;

import org.apache.commons.lang3.Validate;
import org.ehcache.Cache;
import org.ldaptive.BindConnectionInitializer;
import org.ldaptive.ConnectionConfig;
import org.ldaptive.Credential;
import org.ldaptive.DefaultConnectionFactory;
import org.ldaptive.LdapException;
import org.ldaptive.auth.AuthenticationRequest;
import org.ldaptive.auth.AuthenticationResponse;
import org.ldaptive.auth.Authenticator;
import org.ldaptive.auth.SearchDnResolver;
import org.ldaptive.auth.SimpleBindAuthenticationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;

public class BasicAuthenticator {

	private static final Logger log = LoggerFactory.getLogger(BasicAuthenticator.class);

	private final LdapConfiguration ldapConfiguration;
	private final Cache<String, AuthenticationResult> cache;

	public BasicAuthenticator(
			final LdapConfiguration ldapConfiguration,
			final Cache<String, AuthenticationResult> cache) {


		Validate.notNull(ldapConfiguration, "ldapConfiguration cannot be null");
		Validate.notNull(cache, "cache cannot be null");

		this.ldapConfiguration = ldapConfiguration;
		this.cache = cache;

	}

	public AuthenticationResult authenticate(final String credentialsBase64) {

		final String[] credentials = (new String(Base64.getDecoder().decode(credentialsBase64))).split(":");
		final String username = credentials[0];
		if (this.cache.containsKey(credentialsBase64)) {
			return this.cache.get(credentialsBase64);
		} else {
			final String password = credentials[1];
			final AuthenticationResult authenticationResult = this.authenticateWithLdap(username, password);
			if (authenticationResult.isValid()) {
				this.cache.put(credentialsBase64, authenticationResult);
			}
			return authenticationResult;
		}
	}

	protected AuthenticationResult authenticateWithLdap(
			final String username,
			final String password) {


		final ConnectionConfig connectionConfig = new ConnectionConfig(this.ldapConfiguration.getUrl());
		connectionConfig.setConnectionInitializers(new BindConnectionInitializer(this.ldapConfiguration.getBindUser(), new Credential(this.ldapConfiguration.getBindPassword())));

		final SearchDnResolver dnResolver = new SearchDnResolver(new DefaultConnectionFactory(connectionConfig));
		dnResolver.setBaseDn(this.ldapConfiguration.getServiceUserBaseDN());
		dnResolver.setUserFilter("(cn={user})");
		dnResolver.setSubtreeSearch(true);

		final SimpleBindAuthenticationHandler authenticationHandler = new SimpleBindAuthenticationHandler(new DefaultConnectionFactory(connectionConfig));
		final Authenticator authenticator = new Authenticator(dnResolver, authenticationHandler);

		try {
			final AuthenticationRequest authenticationRequest = new AuthenticationRequest(username, new Credential(password));
			final AuthenticationResponse authenticationResponse = authenticator.authenticate(authenticationRequest);

			return getAuthenticationResult(username, authenticationResponse);
		} catch (LdapException e) {
			log.error(e.getMessage(), e);
			throw new BasicAuthenticatorException(e);
		}
	}

	private AuthenticationResult getAuthenticationResult(String username, AuthenticationResponse authenticationResponse) {
		if (authenticationResponse.isSuccess()) {
			return AuthenticationResult
					.success(username, username);
		} else {
			return AuthenticationResult
					.invalid(String.format("Kunne ikke autentisere %s mot %s: %s",
							username,
							this.ldapConfiguration.getServiceUserBaseDN(),
							authenticationResponse.getDiagnosticMessage()));
		}
	}
}