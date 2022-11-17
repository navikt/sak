package no.nav.sak.tokensupport;


import no.nav.security.token.support.core.configuration.MultiIssuerConfiguration;
import no.nav.security.token.support.core.configuration.ProxyAwareResourceRetriever;
import no.nav.security.token.support.core.context.TokenValidationContextHolder;
import no.nav.security.token.support.filter.JwtTokenValidationFilter;
import no.nav.security.token.support.jaxrs.JaxrsTokenValidationContextHolder;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextListener;

@Configuration
public class TokenSupportConfig {


	@Bean
	public MultiIssuerProperties multiIssuerProperties() {
		return new MultiIssuerProperties();
	}

	@Bean
	public FilterRegistrationBean<JwtTokenValidationFilter> oidcTokenValidationFilterBean(
			MultiIssuerConfiguration config) {
		return new FilterRegistrationBean<>(new SakJwtTokenValidationFilter(config));
	}


	@Bean
	public MultiIssuerConfiguration multiIssuerConfiguration(MultiIssuerProperties issuerProperties, ProxyAwareResourceRetriever resourceRetriever) {
		return new MultiIssuerConfiguration(issuerProperties.getIssuer(), resourceRetriever);
	}


	@Bean
	public ProxyAwareResourceRetriever oidcResourceRetriever() {
		return new ProxyAwareResourceRetriever();
	}


	@Bean
	public RequestContextListener requestContextListener() {
		return new RequestContextListener();
	}

	@Bean
	public TokenValidationContextHolder jaxrsContextHolder() {
		return JaxrsTokenValidationContextHolder.getHolder();
	}

}
