package no.nav.sak.tokensupport;

import java.util.Optional;

import no.nav.security.token.support.core.context.TokenValidationContextHolder;
import no.nav.security.token.support.core.jwt.JwtToken;
import no.nav.security.token.support.core.jwt.JwtTokenClaims;
import no.nav.security.token.support.jaxrs.JaxrsTokenValidationContextHolder;

public class TokenUtils {

	public static final String ISSUER_AZUREAD = "azuread";
	private static final TokenValidationContextHolder contextHolder = JaxrsTokenValidationContextHolder.getHolder();

	public static boolean hasTokenForIssuer(String issuer) {

		return contextHolder.getTokenValidationContext() != null ? contextHolder.getTokenValidationContext().hasTokenFor(issuer) : false;
	}

	public static Optional<String> getNavIdent(String issuer) {
		if (!hasTokenForIssuer(issuer)) {
			throw new RuntimeException("No token for issuer");
		}
		JwtToken token = contextHolder.getTokenValidationContext().getJwtToken(ISSUER_AZUREAD);
		JwtTokenClaims claims = token.getJwtTokenClaims();

		return Optional.ofNullable(claims.getStringClaim("NAVident"));
	}

	public static Optional<String> getClientConsumerId(String issuer) {
		if (!hasTokenForIssuer(issuer)) {
			throw new RuntimeException("No token for issuer");
		}
		JwtToken token = contextHolder.getTokenValidationContext().getJwtToken(ISSUER_AZUREAD);
		JwtTokenClaims claims = token.getJwtTokenClaims();

		return Optional.ofNullable(claims.getStringClaim("azp_name"));
	}

}