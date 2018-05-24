package no.nav.sak.infrastruktur;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.UUID;

@EnableApiFilters
@Provider
@Priority(0)
public class CorrelationFilter implements ContainerRequestFilter, ContainerResponseFilter {
    private static final String CORRELATION_HEADER = "X-Correlation-ID";

    private static final Logger log = LoggerFactory.getLogger(CorrelationFilter.class);

    @Override
    public void filter(ContainerRequestContext containerRequestContext) throws IOException {
        String correlationId = containerRequestContext.getHeaderString(CORRELATION_HEADER);
        MDC.put("uuid", UUID.randomUUID().toString());

        if (StringUtils.isBlank(correlationId)) {
            log.warn("Forventet følgende header: {}, avbryter forespørsel", CORRELATION_HEADER);
            containerRequestContext.abortWith(Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(MDC.get("uuid"), String.format("Påkrevd header mangler: %s", CORRELATION_HEADER)))
                .build());
        }

        MDC.put("correlation-id", correlationId);
    }

    @Override
    public void filter(ContainerRequestContext containerRequestContext, ContainerResponseContext containerResponseContext) throws IOException {
        containerResponseContext.getHeaders().add(CORRELATION_HEADER, MDC.get("correlation-id"));
        containerResponseContext.getHeaders().add("X-UUID", MDC.get("uuid"));
        MDC.clear();
    }
}
