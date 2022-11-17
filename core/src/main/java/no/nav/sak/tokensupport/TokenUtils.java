package no.nav.sak.tokensupport;

import java.util.Optional;

import no.nav.security.token.support.core.context.TokenValidationContextHolder;
import no.nav.security.token.support.core.jwt.JwtToken;
import no.nav.security.token.support.core.jwt.JwtTokenClaims;
import no.nav.security.token.support.jaxrs.JaxrsTokenValidationContextHolder;

public class TokenUtils {

	private static TokenValidationContextHolder contextHolder = JaxrsTokenValidationContextHolder.getHolder();

	public static final String ISSUER_AZUREAD = "azuread";

	public static boolean hasTokenForIssuer(String issuer) {

		return contextHolder.getTokenValidationContext() != null ? contextHolder.getTokenValidationContext().hasTokenFor(issuer) : false;
	}

	public static Optional<String> getNavIdent(String issuer) {
		if (!hasTokenForIssuer(issuer)) {
			throw new RuntimeException("No token for issuer");
		}
		JwtToken token = contextHolder.getTokenValidationContext().getJwtToken(ISSUER_AZUREAD);
		JwtTokenClaims claims = token.getJwtTokenClaims();

		Optional<String> navIdent = Optional.of(claims.getStringClaim("NAVident"));

		return navIdent;
	}

	public static Optional<String> getClientConsumerId(String issuer) {
		if (!hasTokenForIssuer(issuer)) {
			throw new RuntimeException("No token for issuer");
		}
		JwtToken token = contextHolder.getTokenValidationContext().getJwtToken(ISSUER_AZUREAD);
		JwtTokenClaims claims = token.getJwtTokenClaims();

		Optional<String> clientConsumer = Optional.of(claims.getStringClaim("azp_name"));

		return clientConsumer;
	}


}
