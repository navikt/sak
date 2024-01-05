package no.nav.sak.infrastruktur.authentication;

import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.jwt.consumer.JwtContext;
import org.jose4j.keys.resolvers.VerificationKeyResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;

/**
 * Valideringsregler er tatt fra FP (Referanseimplementasjon)
 */
public class OidcTokenValidator {
	private static final Logger log = LoggerFactory.getLogger(OidcTokenValidator.class);

	private final Map<String, VerificationKeyResolver> verificationKeyResolvers;

	public OidcTokenValidator(Map<String, VerificationKeyResolver> verificationKeyResolvers) {
		this.verificationKeyResolvers = verificationKeyResolvers;
	}

	public AuthenticationResult validate(String token) {
		if (token == null) {
			return AuthenticationResult.invalid("Missing token (token was null)");
		}

		try {
			String issuer = extractIssuer(token);
			VerificationKeyResolver resolver = verificationKeyResolvers.get(issuer);

			if (resolver == null) {
				return AuthenticationResult.invalid(String.format("No VerificationKeyResolver configured for issuer: %s", issuer));
			}

			JwtConsumer jwtConsumer = new JwtConsumerBuilder()
					.setRequireExpirationTime()
					.setRequireSubject()
					.setExpectedIssuer(issuer)
					.setSkipDefaultAudienceValidation()
					.setVerificationKeyResolver(resolver)
					.build();

			JwtClaims claims = jwtConsumer.processToClaims(token);
			Optional<String> audience = claims.getAudience().stream().findFirst();
			if (audience.isEmpty()) {
				return AuthenticationResult.invalid("Fant ingen audience");
			}

			String navIdent = claims.getClaimValue("NAVident", String.class);
			return AuthenticationResult.success(navIdent != null ? navIdent : claims.getSubject(), audience.get());
		} catch (InvalidJwtException e) {
			return invalidResult("Invalid JWT", e);
		} catch (MalformedClaimException e) {
			return invalidResult("Malformed claim", e);
		}
	}

	private String extractIssuer(String token) throws MalformedClaimException, InvalidJwtException {
		JwtConsumer firstPassJwtConsumer = new JwtConsumerBuilder()
				.setSkipAllValidators()
				.setDisableRequireSignature()
				.setSkipSignatureVerification()
				.build();

		JwtContext jwtContext = firstPassJwtConsumer.process(token);
		return jwtContext.getJwtClaims().getIssuer();
	}

	private AuthenticationResult invalidResult(String msg, Exception e) {
		log.error(msg, e);
		return AuthenticationResult.invalid(msg);
	}
}
