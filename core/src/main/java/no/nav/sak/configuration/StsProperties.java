package no.nav.sak.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotEmpty;

@ConfigurationProperties("sts")
@Validated
public record StsProperties(
		@NotEmpty String issuer,
		@NotEmpty String jwks,
		String user,
		String password
) {
}
