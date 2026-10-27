package no.nav.sak.infrastruktur.rest;

import lombok.Getter;

@Getter
public class SakRuntimeException extends RuntimeException {
	private final String uuid;

	public SakRuntimeException(String uuid, String message, Throwable cause) {
		super(message, cause);
		this.uuid = uuid;
	}

	@Override
	public String getMessage() {
		return super.getMessage() + " [uuid=" + uuid + "]";
	}
}
