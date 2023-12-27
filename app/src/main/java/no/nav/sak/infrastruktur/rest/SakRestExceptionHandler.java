package no.nav.sak.infrastruktur.rest;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import no.nav.sak.infrastruktur.ErrorResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED;
import static org.springframework.http.HttpStatus.NOT_ACCEPTABLE;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.HttpStatus.UNSUPPORTED_MEDIA_TYPE;

@ControllerAdvice
@Slf4j
public class SakRestExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler({UnauthorizedException.class})
	public ResponseEntity<ErrorResponse> unauthorizedExceptionMapper(UnauthorizedException unauthorizedException) {
		return ResponseEntity
				.status(UNAUTHORIZED)
				.body(unauthorizedException.getErrorResponse());
	}

	@ExceptionHandler({ConstraintViolationException.class})
	public ResponseEntity<ErrorResponse> constraintValidationExceptionMapper(ConstraintViolationException constraintValidationException) {
		List<String> violationMessages = constraintValidationException.getConstraintViolations().stream()
				.map(ConstraintViolation::getMessage)
				.collect(Collectors.toList());

		return ResponseEntity
				.status(BAD_REQUEST)
				.body(new ErrorResponse(MDC.get("uuid"), StringUtils.join(violationMessages, ", ")));
	}

	@ExceptionHandler({IllegalStateException.class})
	public ResponseEntity<Object> defaultExceptionMapper(Exception e) {
		log.error("Det oppstod en ukjent feilsituasjon", e);
		return ResponseEntity
				.status(INTERNAL_SERVER_ERROR)
				.body(new ErrorResponse(MDC.get("uuid"), "Det oppstod en ukjent feilsituasjon - se Kibana for årsak"));
	}

	public ResponseEntity<Object> notFoundExceptionMapper(Exception e) {
		log.warn("Mottatt kall mot ressurs som ikke finnes", e);
		return ResponseEntity
				.status(NOT_FOUND)
				.body(new ErrorResponse(MDC.get("uuid"), "Fant ingen ressurs for denne adressen"));
	}

	@ExceptionHandler({ExternalApiException.class})
	public ResponseEntity<Object> externalApiExceptionMapper(ExternalApiException externalApiException) {
		log.error("Det oppstod en feilsituasjon i forbindelse med kall mot et eksternt API", externalApiException);
		return ResponseEntity
				.status(INTERNAL_SERVER_ERROR)
				.body(new ErrorResponse(MDC.get("uuid"), externalApiException.getMessage()));
	}

	@ExceptionHandler({ServiceUnavailableException.class})
	public ResponseEntity<Object> serviceUnavailableExceptionMapper(ServiceUnavailableException serviceUnavailableException) {
		return ResponseEntity
				.status(SERVICE_UNAVAILABLE)
				.body(new ErrorResponse(MDC.get("uuid"), serviceUnavailableException.getMessage()));
	}
	@Override
	protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		log.info("Mottatt kall mot ressurs som ikke støttes");
		return ResponseEntity
				.status(METHOD_NOT_ALLOWED)
				.body(new ErrorResponse(MDC.get("uuid"), "Angitt operasjon er ikke tillatt"));
	}
	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		log.info("Mottatt kall mot ressurs som ikke støttes");
		return ResponseEntity
				.status(BAD_REQUEST)
				.body(new ErrorResponse(MDC.get("uuid"), ex.getMessage()));
	}

	@Override
	protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		log.info("Konsument brukte Content-Type vi ikke støtter", ex);
		return ResponseEntity
				.status(UNSUPPORTED_MEDIA_TYPE)
				.body(new ErrorResponse(MDC.get("uuid"), ex.getMessage()));
	}

	@Override
	protected ResponseEntity<Object> handleHttpMediaTypeNotAcceptable(HttpMediaTypeNotAcceptableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		log.info("Konsument forespurte Accept som ikke er støttet", ex);
		return ResponseEntity
				.status(NOT_ACCEPTABLE)
				.body(new ErrorResponse(MDC.get("uuid"), "MediaType not acceptable"));
	}

	@Override
	protected ResponseEntity<Object> handleMissingPathVariable(MissingPathVariableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		log.info("Manglende path variabel", ex);
		return ResponseEntity
				.status(BAD_REQUEST)
				.body(new ErrorResponse(MDC.get("uuid"), ex.getMessage()));
	}

	@Override
	protected ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		log.info("Manglende parameter i request", ex);
		return ResponseEntity
				.status(BAD_REQUEST)
				.body(new ErrorResponse(MDC.get("uuid"), ex.getMessage()));
	}

	@Override
	protected ResponseEntity<Object> handleNoHandlerFoundException(NoHandlerFoundException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		return this.notFoundExceptionMapper(ex);
	}
	@Override
	protected ResponseEntity<Object> handleNoResourceFoundException(NoResourceFoundException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		return this.notFoundExceptionMapper(ex);
	}
}
