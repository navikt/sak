package no.nav.sak.validering;

import no.nav.sak.infrastruktur.ErrorResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import java.util.List;
import java.util.stream.Collectors;

public class ConstraintValidationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {
    @Override
    public Response toResponse(ConstraintViolationException e) {
        List<String> violationMessages = e.getConstraintViolations().stream()
            .map(ConstraintViolation::getMessage)
            .collect(Collectors.toList());

        return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorResponse(MDC.get("uuid"),
            StringUtils.join(violationMessages, ", "))).build();
    }
}
