package no.nav.sak.tokensupport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import no.nav.security.token.support.core.context.TokenValidationContext;
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

	public static String getNavIdent(String issuer) {
		if (!hasTokenForIssuer(issuer)) {
			throw new RuntimeException("No token for issuer");
		}
		JwtToken token = contextHolder.getTokenValidationContext().getJwtToken(ISSUER_AZUREAD);
		JwtTokenClaims claims = token.getJwtTokenClaims();

		Optional<String> fnr = Optional.of(claims.getStringClaim("NAVident"));

		return fnr.orElseThrow(() -> new RuntimeException("Missing NAVident claim"));
	}

	public static String getConsumerId(String issuer) {
		if (!hasTokenForIssuer(issuer)) {
			throw new RuntimeException("No token for issuer");
		}
		JwtToken token = contextHolder.getTokenValidationContext().getJwtToken(ISSUER_AZUREAD);
		JwtTokenClaims claims = token.getJwtTokenClaims();

		Optional<String> aud = claims.getAsList("aud").stream().findAny();

		return aud.orElseThrow(() -> new RuntimeException("Missing consumer id"));
	}
    

}
