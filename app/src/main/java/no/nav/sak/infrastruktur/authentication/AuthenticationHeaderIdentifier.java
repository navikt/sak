package no.nav.sak.infrastruktur.authentication;

public enum AuthenticationHeaderIdentifier {
	SAML("Saml"),
	BASIC("Basic"),
	OIDC("Bearer");

	private final String headerKey;

	AuthenticationHeaderIdentifier(String headerKey) {
		this.headerKey = headerKey;
	}

	public String getValue() {
		return headerKey;
	}
}
