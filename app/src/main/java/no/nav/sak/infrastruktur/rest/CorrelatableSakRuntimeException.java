package no.nav.sak.infrastruktur.rest;

import lombok.Getter;

@Getter
public class CorrelatableSakRuntimeException extends RuntimeException {
	private final String correlationId;
	private final String uuid;

	public CorrelatableSakRuntimeException(String correlationId, String uuid, String message, Throwable cause) {
		super(message, cause);
		this.correlationId = correlationId;
		this.uuid = uuid;
	}

	@Override
	public String getMessage() {
		return super.getMessage() + "[correlationId=" + correlationId + "] [uuid=" + uuid + "]";
	}
}
