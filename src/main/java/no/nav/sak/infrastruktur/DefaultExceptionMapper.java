package no.nav.sak.infrastruktur;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

public class DefaultExceptionMapper implements ExceptionMapper<Exception> {
    private static final Logger log = LoggerFactory.getLogger(DefaultExceptionMapper.class);

    @Override
    public Response toResponse(Exception e) {
        log.error("Det oppstod en ukjent feilsituasjon", e);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .entity(new ErrorResponse(MDC.get("uuid"), "Det oppstod en ukjent feilsituasjon - se Kibana for årsak"))
            .build();
    }
}
