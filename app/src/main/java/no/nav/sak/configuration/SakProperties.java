package no.nav.sak.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Set;

@ConfigurationProperties("sak")
@Data
public class SakProperties {
	/**
	 * Apper som kan kalle sak med Basic auth
	 */
	private Set<String> basicAuthTilgang = Set.of();
}
