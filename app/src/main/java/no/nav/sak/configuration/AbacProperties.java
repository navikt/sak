package no.nav.sak.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotEmpty;

@ConfigurationProperties("abac.pdp")
@Validated
public record AbacProperties(
		@NotEmpty String endpoint,
		Integer readTimeout) {
}
