package no.nav.sak.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotEmpty;

@ConfigurationProperties("srvsak")
@Validated
public record ServiceuserProperties(
		@NotEmpty String username,
		@NotEmpty String password
) {
}
