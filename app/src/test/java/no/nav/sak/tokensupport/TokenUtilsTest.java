package no.nav.sak.tokensupport;

import no.nav.sak.infrastruktur.oicd.JwtClaimsTestData;
import no.nav.sak.infrastruktur.oicd.JwtTestData;
import no.nav.security.token.support.core.context.TokenValidationContext;
import no.nav.security.token.support.core.jwt.JwtToken;
import no.nav.security.token.support.jaxrs.JaxrsTokenValidationContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static no.nav.sak.tokensupport.TokenUtils.ISSUER_ENTRA;
import static org.assertj.core.api.Assertions.assertThat;

class TokenUtilsTest {

	@AfterEach
	void clearTokenValidationContext() {
		JaxrsTokenValidationContextHolder.INSTANCE.getHolder().setTokenValidationContext(null);
	}

	@ParameterizedTest
	@ValueSource(strings = {"",
		"app",
		"username_39_chars_xxxxxxxxxxxxxxxxxxxxx",
		"username_40_chars_xxxxxxxxxxxxxxxxxxxxxx",
		"username_54_chars_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
		"username_100_chars_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
		})
	void getUsernameShouldTruncateUsernameToMax40Chars(String originalUsername) {
		var claims = new JwtClaimsTestData().build();
		claims.setSubject("client");
		claims.setClaim("oid", "client");
		claims.setClaim("azp_name", originalUsername);
		var token = new JwtToken(new JwtTestData().claims(claims).build());
		JaxrsTokenValidationContextHolder.INSTANCE.getHolder()
				.setTokenValidationContext(new TokenValidationContext(Map.of(ISSUER_ENTRA, token)));

		var username = TokenUtils.getUsername(ISSUER_ENTRA);

		assertThat(username.orElseThrow()).hasSizeLessThanOrEqualTo(40);
	}
}