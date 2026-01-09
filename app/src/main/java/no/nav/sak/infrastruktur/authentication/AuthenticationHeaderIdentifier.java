package no.nav.sak.infrastruktur.authentication;

public enum AuthenticationHeaderIdentifier {
	BASIC("Basic"),
	BEARER("Bearer");

	private final String headerKey;

	AuthenticationHeaderIdentifier(String headerKey) {
		this.headerKey = headerKey;
	}

	public String getValue() {
		return headerKey;
	}
}
