package no.nav.sak.infrastruktur.rest;

import no.nav.sak.infrastruktur.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

public class ExternalApiExceptionMapper implements ExceptionMapper<ExternalApiException> {
    private static final Logger log = LoggerFactory.getLogger(ExternalApiException.class);

    @Override
    public Response toResponse(ExternalApiException e) {
        log.error("Det oppstod en feilsituasjon i forbindelse med kall mot et eksternt API", e);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .entity(new ErrorResponse(MDC.get("uuid"), e.getMessage()))
            .build();
    }
}
