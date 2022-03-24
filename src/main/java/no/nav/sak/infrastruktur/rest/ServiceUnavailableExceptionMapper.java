package no.nav.sak.infrastruktur.rest;

import no.nav.sak.infrastruktur.ErrorResponse;
import org.slf4j.MDC;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

public class ServiceUnavailableExceptionMapper implements ExceptionMapper<ServiceUnavailableException> {

    @Override
    public Response toResponse(ServiceUnavailableException e) {
        return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(
            new ErrorResponse(MDC.get("uuid"), e.getMessage()))
            .build();
    }
}
