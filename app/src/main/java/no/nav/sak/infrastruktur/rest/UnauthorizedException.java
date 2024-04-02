package no.nav.sak.infrastruktur.rest;

import no.nav.sak.infrastruktur.ErrorResponse;

public class UnauthorizedException extends SakRestException {

	public UnauthorizedException(ErrorResponse errorResponse) {
		super("Unauthorized", errorResponse, null);
	}

}
