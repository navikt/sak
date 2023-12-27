package no.nav.sak.infrastruktur.rest;

import no.nav.sak.infrastruktur.ErrorResponse;

public class SakRestException extends RuntimeException {
	protected final ErrorResponse errorResponse;

	public SakRestException(ErrorResponse errorResponse) {
		this.errorResponse = errorResponse;
	}

	public ErrorResponse getErrorResponse() {
		return errorResponse;
	}
}
