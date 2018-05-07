package no.nav.sak.infrastruktur.authentication;


import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
import no.nav.sak.infrastruktur.EnableApiFilters;
import no.nav.sak.infrastruktur.ErrorResponse;
import no.nav.sikkerhet.authentication.AuthenticationResult;
import no.nav.sikkerhet.authentication.Authenticator;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import java.io.IOException;

import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.trim;

@Provider
@EnableApiFilters
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationFilter implements ContainerRequestFilter, ContainerResponseFilter {
    public static final String REQUEST_USERNAME = "username";
    public static final String REQUEST_CONSUMERID = "consumerid";

    private static final Histogram authenticationHistogram = Histogram.build("authentication_duration_seconds", "Authentication duration in seconds")
        .labelNames("authidentifier")
        .register();

    private static final Counter authCounter = Counter.build("authentication_counter", "Antall autentiseringer")
        .labelNames("consumerid", "valid", "authidentifier").register();

    private static final Logger log = LoggerFactory.getLogger(AuthenticationFilter.class);

    private final Authenticator authenticator;

    public AuthenticationFilter(Authenticator authenticator) {
        this.authenticator = authenticator;
    }

    @Override
    public void filter(ContainerRequestContext ctx) {
        String authHeader = ctx.getHeaderString(AUTHORIZATION);
        String authIdentifier = StringUtils.substringBefore(trim(authHeader), " ");

        Histogram.Timer timer = authenticationHistogram
            .labels(
                defaultString(authIdentifier, "N/A"))
            .startTimer();

        try {
            AuthenticationResult result = authenticator.authenticate(authHeader);
            if (!result.isValid()) {
                log.warn("Autentisering feilet: {}", result.getErrorMessage());
                abortAsUnauthorized(ctx);
                authCounter.labels(
                    defaultString(result.getConsumerId(), "N/A"),
                    "NO",
                    defaultString(authIdentifier, "N/A"));
            }
            MDC.put(REQUEST_CONSUMERID, result.getConsumerId());
            ctx.setProperty(REQUEST_CONSUMERID, result.getConsumerId());
            ctx.setProperty(REQUEST_USERNAME, result.getUser());
            authCounter.labels(defaultString(result.getConsumerId(), "N/A"), "YES", defaultString(authIdentifier, "N/A")).inc();
        } finally {
            timer.observeDuration();
        }
    }

    private void abortAsUnauthorized(ContainerRequestContext ctx) {
        ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED)
            .entity(new ErrorResponse(MDC.get("uuid"), "Autentisering feilet - se Kibana for årsak"))
            .build());
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        MDC.remove(REQUEST_CONSUMERID);
    }
}
