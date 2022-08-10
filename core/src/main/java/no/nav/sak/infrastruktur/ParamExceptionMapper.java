package no.nav.sak.infrastruktur;

import org.glassfish.jersey.server.ParamException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

public class ParamExceptionMapper implements ExceptionMapper<ParamException> {
    private static Logger log = LoggerFactory.getLogger(ParamExceptionMapper.class);

    @Override
    public Response toResponse(ParamException e) {
        log.warn("Mottatt kall med ugyldige parametre");
        return Response.status(Response.Status.NOT_FOUND).entity(
            new ErrorResponse(MDC.get("uuid"), "Mottatt kall med ugyldige parametre"))
            .build();
    }
}
