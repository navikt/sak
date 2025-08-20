package no.nav.sak.tokensupport;

import no.nav.security.token.support.core.context.TokenValidationContextHolder;
import no.nav.security.token.support.core.jwt.JwtToken;
import no.nav.security.token.support.core.jwt.JwtTokenClaims;
import no.nav.security.token.support.jaxrs.JaxrsTokenValidationContextHolder;

import java.util.Optional;

import static org.springframework.util.StringUtils.truncate;

public class TokenUtils {

	private static final int USERNAME_MAX_LENGTH = 40;
	private static final String SUBJECT_CLAIM = "sub";
	private static final String OID_CLAIM = "oid";
	private static final String AZP_NAME_CLAIM = "azp_name";
	private static final String NAVIDENT_CLAIM = "NAVident";

	public static final String ISSUER_AZUREAD = "azuread";

	private static final TokenValidationContextHolder contextHolder = JaxrsTokenValidationContextHolder.getHolder();

	public static boolean hasTokenForIssuer(String issuer) {
		return contextHolder.getTokenValidationContext() != null && contextHolder.getTokenValidationContext().hasTokenFor(issuer);
	}

	public static boolean hasClientCredentialsToken() {
		if (!hasTokenForIssuer(ISSUER_AZUREAD)) {
			return false;
		}
		JwtToken token = contextHolder.getTokenValidationContext().getJwtToken(ISSUER_AZUREAD);
		JwtTokenClaims claims = token.getJwtTokenClaims();
		return isClientCredentialsToken(claims);
	}

	public static Optional<String> getUsername(String issuer) {
		if (!hasTokenForIssuer(issuer)) {
			throw new RuntimeException("No token for issuer");
		}
		JwtToken token = contextHolder.getTokenValidationContext().getJwtToken(ISSUER_AZUREAD);
		JwtTokenClaims claims = token.getJwtTokenClaims();

		if (isClientCredentialsToken(claims)) {
			String azpName = claims.getStringClaim(AZP_NAME_CLAIM);
			String parsedAzpName = parseAzpNameClaim(azpName);
			return Optional.of(truncate(parsedAzpName, USERNAME_MAX_LENGTH));
		}

		return Optional.ofNullable(claims.getStringClaim(NAVIDENT_CLAIM));
	}

	public static Optional<String> getClientConsumerId(String issuer) {
		if (!hasTokenForIssuer(issuer)) {
			throw new RuntimeException("No token for issuer");
		}
		JwtToken token = contextHolder.getTokenValidationContext().getJwtToken(ISSUER_AZUREAD);
		JwtTokenClaims claims = token.getJwtTokenClaims();

		return Optional.ofNullable(claims.getStringClaim(AZP_NAME_CLAIM));
	}

	private static boolean isClientCredentialsToken(JwtTokenClaims claims) {
		var sub = claims.getStringClaim(SUBJECT_CLAIM);
		var oid = claims.getStringClaim(OID_CLAIM);

		if (sub == null || oid == null) {
			return false;
		}

		return sub.equals(oid);
	}

	private static String parseAzpNameClaim(String azpnameClaim) {
		try {
			var split = azpnameClaim.split(":");
			return split[1] + ":" + split[2];
		} catch (Exception e) {
			return azpnameClaim;
		}
	}

}