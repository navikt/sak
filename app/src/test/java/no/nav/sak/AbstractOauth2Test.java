package no.nav.sak;

import no.nav.security.mock.oauth2.MockOAuth2Server;
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback;
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier.DEFAULT_MAX_CLOCK_SKEW_SECONDS;

@EnableMockOAuth2Server
public abstract class AbstractOauth2Test {

	private static final String ISSUER = "entra";
	private static final String AZP_NAME = "itest:teamdokumenthandtering:sak";
	private static final String APP_CLAIM_SUB = UUID.randomUUID().toString();

	@Autowired
	public MockOAuth2Server mockOAuth2Server;

	public String oboToken() {
		return jwt(entraOboClaims());
	}

	public String expiredOboToken() {
		return jwt(entraOboClaims(), -(DEFAULT_MAX_CLOCK_SKEW_SECONDS + 1));
	}

	public String clientCredentialsToken() {
		return jwt(entraClientCredentialsClaims());
	}

	private String jwt(Map<String, String> claims, int ...expiry) {

		return mockOAuth2Server.issueToken(
				ISSUER,
				"app-clientid",
				new DefaultOAuth2TokenCallback(
						ISSUER,
						"subject",
						"JWT",
						List.of("aud-localhost"),
						claims,
						expiry.length == 0 ? 60 : expiry[0]
				)
		).serialize();
	}

	private static Map<String, String> entraOboClaims() {
		return Map.of(
				"azp_name", "itest:team:app",
				"NAVident", "Z123456",
				"oid", UUID.randomUUID().toString(),
				"name", "Bjarne Betjent",
				"scp", "api_admin defaultaccess"
		);
	}

	private static Map<String, String> entraClientCredentialsClaims() {
		return Map.of(
				"azp_name", AZP_NAME,
				"sub", APP_CLAIM_SUB,
				"oid", APP_CLAIM_SUB
		);
	}

}
