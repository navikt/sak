package no.nav.sak.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotEmpty;

@ConfigurationProperties("sakds")
@Validated
public record DatasourceProperties(
		@NotEmpty String username,
		String password,
		@NotEmpty String url
) {
}
