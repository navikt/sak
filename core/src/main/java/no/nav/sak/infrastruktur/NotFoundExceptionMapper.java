package no.nav.sak.infrastruktur;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

public class NotFoundExceptionMapper implements ExceptionMapper<NotFoundException> {
    private static Logger log = LoggerFactory.getLogger(NotFoundExceptionMapper.class);

    @Override
    public Response toResponse(NotFoundException e) {
        log.warn("Mottatt kall mot ressurs som ikke finnes");
        return Response.status(Response.Status.NOT_FOUND).entity(
            new ErrorResponse(MDC.get("uuid"), "Fant ingen ressurs for denne adressen"))
            .build();
    }
}
