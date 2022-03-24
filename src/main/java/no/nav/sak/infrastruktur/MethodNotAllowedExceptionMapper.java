package no.nav.sak.infrastruktur;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

public class MethodNotAllowedExceptionMapper implements ExceptionMapper<NotAllowedException> {
    private static Logger log = LoggerFactory.getLogger(MethodNotAllowedExceptionMapper.class);

    @Override
    public Response toResponse(NotAllowedException e) {
        log.warn("Mottatt kall mot ressurs som ikke støttes");
        return Response.status(Response.Status.METHOD_NOT_ALLOWED).entity(
            new ErrorResponse(MDC.get("uuid"), "Angitt operasjon er ikke tillatt"))
            .build();
    }
}
