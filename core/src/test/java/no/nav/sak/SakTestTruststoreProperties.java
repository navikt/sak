package no.nav.sak;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotEmpty;

@ConfigurationProperties("sak.junit-truststore")
@Validated
public record SakTestTruststoreProperties(
		@NotEmpty String path,
		@NotEmpty String password
) {
}
