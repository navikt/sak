package no.nav.sak.infrastruktur.authentication;


import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.Priority;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Priorities;
import lombok.extern.slf4j.Slf4j;
import no.nav.resilience.ResilienceConfig;
import no.nav.resilience.ResilienceExecutor;
import no.nav.sak.infrastruktur.ErrorResponse;
import no.nav.sak.infrastruktur.SakOncePerRequestFilter;
import no.nav.sak.infrastruktur.rest.UnauthorizedException;
import no.nav.sak.tokensupport.TokenUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Objects;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static no.nav.sak.infrastruktur.ContextExtractor.getSubjectType;
import static no.nav.sak.infrastruktur.authentication.AuthenticationHeaderIdentifier.BASIC;
import static no.nav.sak.infrastruktur.authentication.AuthenticationHeaderIdentifier.OIDC;
import static no.nav.sak.infrastruktur.authentication.AuthenticationHeaderIdentifier.SAML;
import static org.apache.commons.lang3.StringUtils.trim;

@Component
@Priority(Priorities.AUTHENTICATION + 1)
@Slf4j
public class AuthenticationFilter extends SakOncePerRequestFilter {
	public static final String CONSUMERID = "consumerid";
	public static final String SUBJECTTYPE = "subjecttype";
	public static final String VALID = "valid";
	public static final String AUTHIDENTIFIER = "authidentifier";
	public static final String REQUEST_USERNAME = "username";
	public static final String REQUEST_CONSUMERID = CONSUMERID;

	private final Counter.Builder authCounter;

	private final ResilienceExecutor<String, AuthenticationResult> resilienceExecutor;
	private final MeterRegistry meterRegistry;

	@Autowired
	public AuthenticationFilter(Authenticator authenticator, ResilienceConfig resilienceConfig, MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;
		this.resilienceExecutor = new ResilienceExecutor<>(authenticator::authenticate, resilienceConfig);
		this.authCounter = Counter.builder("authentication_counter")
				.description("Antall autentiseringer");
	}

	@Override
	public void doFilterInternal(HttpServletRequest httpRequest, HttpServletResponse servletResponse, FilterChain filterChain) throws ServletException, IOException {
		try {
			String authHeader = httpRequest.getHeader(AUTHORIZATION);
			String authIdentifier = StringUtils.substringBefore(trim(authHeader), " ");
			if (TokenUtils.hasTokenForIssuer(TokenUtils.ISSUER_AZUREAD)) {
				authCounter.tags(CONSUMERID, Objects.toString("azureConsumer", "N/A"),
								SUBJECTTYPE, getSubjectType(httpRequest).getValue(),
								VALID, "YES",
								AUTHIDENTIFIER, Objects.toString(authIdentifier, "N/A"))
						.register(meterRegistry)
						.increment();
				MDC.put(REQUEST_CONSUMERID, TokenUtils.getClientConsumerId(TokenUtils.ISSUER_AZUREAD).orElseThrow(this::createUnauthorizedException));
				httpRequest.setAttribute(REQUEST_CONSUMERID, TokenUtils.getClientConsumerId(TokenUtils.ISSUER_AZUREAD).orElseThrow(this::createUnauthorizedException));
				httpRequest.setAttribute(REQUEST_USERNAME, TokenUtils.getNavIdent(TokenUtils.ISSUER_AZUREAD).orElseThrow(this::createUnauthorizedException));
			} else {

				if (!(Objects.equals(authIdentifier, SAML.getValue()) || Objects.equals(authIdentifier, OIDC.getValue()) || Objects.equals(authIdentifier, BASIC.getValue()))) {
					authIdentifier = "N/A";
				}

				AuthenticationResult result = resilienceExecutor.execute(authHeader);
				if (!result.isValid()) {
					authCounter.tags(CONSUMERID, Objects.toString(result.getConsumerId(), "N/A"),
									SUBJECTTYPE, getSubjectType(httpRequest).getValue(),
									VALID, "NO",
									AUTHIDENTIFIER, Objects.toString(authIdentifier, "N/A"))
							.register(meterRegistry)
							.increment();
					throw createUnauthorizedException();
				}
				MDC.put(REQUEST_CONSUMERID, result.getConsumerId());
				httpRequest.setAttribute(REQUEST_CONSUMERID, result.getConsumerId());
				httpRequest.setAttribute(REQUEST_USERNAME, result.getUser());
				authCounter.tags(CONSUMERID, Objects.toString(result.getConsumerId(), "N/A"),
								SUBJECTTYPE, getSubjectType(httpRequest).getValue(),
								VALID, "YES",
								AUTHIDENTIFIER, Objects.toString(authIdentifier, "N/A"))
						.register(meterRegistry)
						.increment();

			}
			filterChain.doFilter(httpRequest, servletResponse);
		} finally {
			MDC.remove(REQUEST_CONSUMERID);
		}
	}

	private UnauthorizedException createUnauthorizedException() {
		return new UnauthorizedException(new ErrorResponse(MDC.get("uuid"), "Autentisering feilet - se Kibana for årsak"));
	}
}
