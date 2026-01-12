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

	private static final String ISSUER_ENTRA = "entra";
	private static final String ISSUER_STS = "sts";
	private static final String AZP_NAME = "itest:junit:system";
	private static final String APP_CLAIM_SUB = UUID.randomUUID().toString();
	public static final List<String> ENTRA_AUDIENCE = List.of("itest:junit:sak");

	@Autowired
	public MockOAuth2Server mockOAuth2Server;

	public String oboToken() {
		return jwt(entraOboClaims(), ISSUER_ENTRA, ENTRA_AUDIENCE);
	}

	public String expiredOboToken() {
		return jwt(entraOboClaims(), ISSUER_ENTRA, ENTRA_AUDIENCE, -(DEFAULT_MAX_CLOCK_SKEW_SECONDS + 1));
	}

	public String clientCredentialsToken() {
		return jwt(entraClientCredentialsClaims(), ISSUER_ENTRA, ENTRA_AUDIENCE);
	}

	public String stsToken() {
		return jwt(stsClaims(), ISSUER_STS, List.of("srvjunit"));
	}

	private Map<String, Object> stsClaims() {
		return Map.of(
				"sub", "srvjunit",
				"azp", "srvjunit"
		);
	}

	private static Map<String, Object> entraOboClaims() {
		return Map.of(
				"azp_name", "itest:team:app",
				"NAVident", "Z123456",
				"oid", UUID.randomUUID().toString(),
				"name", "Bjarne Betjent",
				"scp", "api_admin defaultaccess"
		);
	}

	private static Map<String, Object> entraClientCredentialsClaims() {
		return Map.of(
				"azp_name", AZP_NAME,
				"sub", APP_CLAIM_SUB,
				"oid", APP_CLAIM_SUB
		);
	}

	private String jwt(Map<String, Object> claims, String issuer, List<String> audience, int ...expiry) {

		return mockOAuth2Server.issueToken(
				issuer,
				"app-clientid",
				new DefaultOAuth2TokenCallback(
						issuer,
						"subject",
						"JWT",
						audience,
						claims,
						expiry.length == 0 ? 60 : expiry[0]
				)
		).serialize();
	}

}
