package no.nav.sak.tokensupport;

import no.nav.security.token.support.core.configuration.IssuerProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;


//tatt ut av token-support-spring
@ConfigurationProperties(prefix = "no.nav.security.jwt")
public class MultiIssuerProperties {


	private final Map<String, IssuerProperties> issuer = new HashMap<>();

	public Map<String, IssuerProperties> getIssuer() {
		return issuer;
	}

	@Override
	public String toString() {
		return "MultiIssuerConfigurationProperties [issuer=" + issuer + "]";
	}
}