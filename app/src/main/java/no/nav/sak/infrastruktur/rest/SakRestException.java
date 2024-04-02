package no.nav.sak.infrastruktur.rest;

import no.nav.sak.infrastruktur.ErrorResponse;
import org.slf4j.MDC;

import static no.nav.sak.infrastruktur.CorrelationFilter.MDC_CORRELATION_ID;
import static no.nav.sak.infrastruktur.CorrelationFilter.MDC_UUID;

public abstract class SakRestException extends CorrelatableSakRuntimeException {
	protected final ErrorResponse errorResponse;

	public SakRestException(String message, ErrorResponse errorResponse, Throwable cause) {
		super(MDC.get(MDC_CORRELATION_ID), MDC.get(MDC_UUID), message, cause);
		this.errorResponse = errorResponse;
	}

	public ErrorResponse getErrorResponse() {
		return errorResponse;
	}
}
