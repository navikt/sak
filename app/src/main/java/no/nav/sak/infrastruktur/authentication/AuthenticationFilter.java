package no.nav.sak.infrastruktur.authentication;


import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
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
	public static final String REQUEST_USERNAME = "username";
	public static final String REQUEST_CONSUMERID = "consumerid";

	private static final Histogram authenticationHistogram = Histogram.build("authentication_duration_seconds", "Authentication duration in seconds")
			.labelNames("authidentifier")
			.register();

	private static final Counter authCounter = Counter.build("authentication_counter", "Antall autentiseringer")
			.labelNames("consumerid", "subjecttype", "valid", "authidentifier").register();

	private final ResilienceExecutor<String, AuthenticationResult> resilienceExecutor;

	@Autowired
	public AuthenticationFilter(Authenticator authenticator, ResilienceConfig resilienceConfig) {
		this.resilienceExecutor = new ResilienceExecutor<>(authenticator::authenticate, resilienceConfig);
	}

	@Override
	public void doFilterInternal(HttpServletRequest httpRequest, HttpServletResponse servletResponse, FilterChain filterChain) throws ServletException, IOException {
		try {
			String authHeader = httpRequest.getHeader(AUTHORIZATION);
			String authIdentifier = StringUtils.substringBefore(trim(authHeader), " ");
			Histogram.Timer timer;
			if (TokenUtils.hasTokenForIssuer(TokenUtils.ISSUER_AZUREAD)) {
				timer = authenticationHistogram
						.labels(
								Objects.toString(TokenUtils.ISSUER_AZUREAD, "N/A"))
						.startTimer();
				timer.observeDuration();
				authCounter.labels(Objects.toString("azureConsumer", "N/A"),
						getSubjectType(httpRequest).getValue(),
						"YES",
						Objects.toString(authIdentifier, "N/A")).inc();
				MDC.put(REQUEST_CONSUMERID, TokenUtils.getClientConsumerId(TokenUtils.ISSUER_AZUREAD).orElseThrow(this::createUnauthorizedException));
				httpRequest.setAttribute(REQUEST_CONSUMERID, TokenUtils.getClientConsumerId(TokenUtils.ISSUER_AZUREAD).orElseThrow(this::createUnauthorizedException));
				httpRequest.setAttribute(REQUEST_USERNAME, TokenUtils.getNavIdent(TokenUtils.ISSUER_AZUREAD).orElseThrow(this::createUnauthorizedException));
			} else {

				if (!(Objects.equals(authIdentifier, SAML.getValue()) || Objects.equals(authIdentifier, OIDC.getValue()) || Objects.equals(authIdentifier, BASIC.getValue()))) {
					authIdentifier = "N/A";
				}
				timer = authenticationHistogram
						.labels(
								Objects.toString(authIdentifier, "N/A"))
						.startTimer();

				try {
					AuthenticationResult result = resilienceExecutor.execute(authHeader);
					if (!result.isValid()) {
						authCounter.labels(
								Objects.toString(result.getConsumerId(), "N/A"),
								getSubjectType(httpRequest).getValue(),
								"NO",
								Objects.toString(authIdentifier, "N/A")).inc();
						throw createUnauthorizedException();
					}
					MDC.put(REQUEST_CONSUMERID, result.getConsumerId());
					httpRequest.setAttribute(REQUEST_CONSUMERID, result.getConsumerId());
					httpRequest.setAttribute(REQUEST_USERNAME, result.getUser());
					authCounter.labels(Objects.toString(result.getConsumerId(), "N/A"),
							getSubjectType(httpRequest).getValue(),
							"YES",
							Objects.toString(authIdentifier, "N/A")).inc();
				} finally {
					timer.observeDuration();
				}

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
