package no.nav.sak.tokensupport;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import lombok.extern.slf4j.Slf4j;
import no.nav.security.token.support.core.configuration.MultiIssuerConfiguration;
import no.nav.security.token.support.core.validation.JwtTokenValidationHandler;
import no.nav.security.token.support.filter.JwtTokenValidationFilter;
import no.nav.security.token.support.jaxrs.JaxrsTokenValidationContextHolder;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static org.apache.commons.lang3.StringUtils.trim;

@Component
@Priority(Priorities.AUTHENTICATION)
@Slf4j
public class SakJwtTokenValidationFilter extends JwtTokenValidationFilter {

	private static final String AUTORIZATION_BEARER = "Bearer";

	public SakJwtTokenValidationFilter(MultiIssuerConfiguration oidcConfig) {
		super(new JwtTokenValidationHandler(oidcConfig), JaxrsTokenValidationContextHolder.getHolder());
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		if (request instanceof HttpServletRequest httpServletRequest) {
			String authorizationHeader = httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION);
			String authorizationType = StringUtils.substringBefore(trim(authorizationHeader), " ");

			if (AUTORIZATION_BEARER.equals(authorizationType)) {
				super.doFilter(request, response, chain);
			} else {
				chain.doFilter(request, response);
			}
		} else {
			chain.doFilter(request, response);
		}
	}
}
