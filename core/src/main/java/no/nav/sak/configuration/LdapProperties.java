package no.nav.sak.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotEmpty;

@ConfigurationProperties("ldap")
@Validated
public record LdapProperties(
		@NotEmpty String url,
		@NotEmpty String username,
		@NotEmpty String password,
		@NotEmpty String serviceuserBasedn
) {
}
