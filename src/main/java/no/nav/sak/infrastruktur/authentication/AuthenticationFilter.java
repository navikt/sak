package no.nav.sak.infrastruktur.authentication;


import no.nav.sak.infrastruktur.EnableApiFilters;
import no.nav.sak.infrastruktur.ErrorResponse;
import no.nav.sikkerhet.authentication.AuthenticationResult;
import no.nav.sikkerhet.authentication.Authenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;

@Provider
@EnableApiFilters
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationFilter implements ContainerRequestFilter {
    public static final String REQUEST_USERNAME = "username";
    public static final String REQUEST_CONSUMERID = "consumerid";

    private static final Logger log = LoggerFactory.getLogger(AuthenticationFilter.class);

    private final Authenticator authenticator;

    public AuthenticationFilter(Authenticator authenticator) {
        this.authenticator = authenticator;
    }

    @Override
    public void filter(ContainerRequestContext ctx) {
        AuthenticationResult result = authenticator.authenticate(ctx.getHeaderString(AUTHORIZATION));
        if (!result.isValid()) {
            log.warn("Autentisering feilet: {}", result.getErrorMessage());
            abortAsUnauthorized(ctx);
        }

        ctx.setProperty(REQUEST_CONSUMERID, result.getConsumerId());
        ctx.setProperty(REQUEST_USERNAME, result.getUser());
    }

    private void abortAsUnauthorized(ContainerRequestContext ctx) {
        ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED)
            .entity(new ErrorResponse(MDC.get("uuid"), "Autentisering feilet - se Kibana for årsak"))
            .build());
    }
}
