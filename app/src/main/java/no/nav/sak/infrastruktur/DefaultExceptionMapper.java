package no.nav.sak.infrastruktur;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

@Slf4j
public class DefaultExceptionMapper implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(Exception e) {
        log.error("Det oppstod en ukjent feilsituasjon", e);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .entity(new ErrorResponse(MDC.get("uuid"), "Det oppstod en ukjent feilsituasjon - se Kibana for årsak"))
            .build();
    }
}
