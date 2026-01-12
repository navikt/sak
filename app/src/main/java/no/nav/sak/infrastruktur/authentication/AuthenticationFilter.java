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
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Objects;
import java.util.Set;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static no.nav.sak.infrastruktur.ContextExtractor.getSubjectType;
import static no.nav.sak.infrastruktur.authentication.AuthenticationHeaderIdentifier.BASIC;
import static no.nav.sak.infrastruktur.authentication.AuthenticationHeaderIdentifier.BEARER;
import static no.nav.sak.tokensupport.TokenUtils.ISSUER_ENTRA;
import static no.nav.sak.tokensupport.TokenUtils.ISSUER_STS;
import static no.nav.sak.tokensupport.TokenUtils.getClientConsumerId;
import static no.nav.sak.tokensupport.TokenUtils.getUsername;
import static no.nav.sak.tokensupport.TokenUtils.hasTokenForIssuer;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.apache.commons.lang3.StringUtils.trim;

@Component
@Priority(Priorities.AUTHENTICATION + 1)
@Slf4j
public class AuthenticationFilter extends SakOncePerRequestFilter {
	public static final String REQUEST_USERNAME = "username";
	public static final String CONSUMERID = "consumerid";
	public static final String REQUEST_CONSUMERID = CONSUMERID;
	private static final String SUBJECTTYPE = "subjecttype";
	private static final String VALID = "valid";
	private static final String AUTHIDENTIFIER = "authidentifier";
	private static final Set<String> VALID_AUTH_IDENTIFIERS = Set.of(BASIC.getValue(), BEARER.getValue());
	private static final String NOT_AVAILABLE = "N/A";
	private static final String VALID_NO = "NO";
	private static final String VALID_YES = "YES";

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
			String authIdentifier = mapAuthIdentifier(authHeader);
			if (hasTokenForIssuer(ISSUER_ENTRA)) {
				handleBearerMetadata(ISSUER_ENTRA, httpRequest, authIdentifier);
			} else if (hasTokenForIssuer(ISSUER_STS)) {
				handleBearerMetadata(ISSUER_STS, httpRequest, authIdentifier);
			} else {
				AuthenticationResult result = resilienceExecutor.execute(authHeader);
				if (!result.isValid()) {
					incrementAuthCounter(result.getConsumerId(), httpRequest, VALID_NO, authIdentifier);
					throw createUnauthorizedException("Kunne ikke autorisere bruker");
				}
				MDC.put(REQUEST_CONSUMERID, result.getConsumerId());
				httpRequest.setAttribute(REQUEST_CONSUMERID, result.getConsumerId());
				httpRequest.setAttribute(REQUEST_USERNAME, result.getUser());
				incrementAuthCounter(result.getConsumerId(), httpRequest, VALID_YES, authIdentifier);
			}
			filterChain.doFilter(httpRequest, servletResponse);
		} finally {
			MDC.remove(REQUEST_CONSUMERID);
		}
	}

	private static String mapAuthIdentifier(String authHeader) {
		String authIdentifier = substringBefore(trim(authHeader), " ");
		if (authIdentifier == null || !VALID_AUTH_IDENTIFIERS.contains(authIdentifier)) {
			return NOT_AVAILABLE;
		}
		return authIdentifier;
	}

	private void handleBearerMetadata(String issuerId, HttpServletRequest httpRequest, String authIdentifier) {
		String consumerId = getClientConsumerId(issuerId).orElseThrow(() -> createUnauthorizedException("Kunne ikke hente consumerId fra token for issuerId=" + issuerId));
		MDC.put(REQUEST_CONSUMERID, consumerId);
		httpRequest.setAttribute(REQUEST_CONSUMERID, consumerId);
		httpRequest.setAttribute(REQUEST_USERNAME, getUsername(issuerId).orElseThrow(() -> createUnauthorizedException("Kunne ikke hente username fra token for issuerId=" + issuerId)));
		incrementAuthCounter(consumerId, httpRequest, VALID_YES, authIdentifier);
	}

	private void incrementAuthCounter(String consumerId, HttpServletRequest httpRequest, String valid, String authIdentifier) {
		authCounter.tags(CONSUMERID, Objects.toString(consumerId, NOT_AVAILABLE),
						SUBJECTTYPE, getSubjectType(httpRequest).getValue(),
						VALID, valid,
						AUTHIDENTIFIER, Objects.toString(authIdentifier, NOT_AVAILABLE))
				.register(meterRegistry)
				.increment();
	}

	private UnauthorizedException createUnauthorizedException(String reason) {
		return new UnauthorizedException(new ErrorResponse(MDC.get("uuid"), "Autentisering feilet. " + reason));
	}
}
