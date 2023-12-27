package no.nav.sak.infrastruktur;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import org.springframework.http.ResponseEntity;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@Slf4j
public class DefaultExceptionMapper {

    public ResponseEntity<ErrorResponse> toResponse(Exception e) {
        log.error("Det oppstod en ukjent feilsituasjon", e);
        return new ResponseEntity<>(
				new ErrorResponse(MDC.get("uuid"), "Det oppstod en ukjent feilsituasjon - se Kibana for årsak"),
				INTERNAL_SERVER_ERROR);
            //.entity(
            //.build();
    }
}
