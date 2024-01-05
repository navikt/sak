package no.nav.sak.infrastruktur.abac;

public enum Decision {
    PERMIT("Permit"), DENY("Deny");

    private String value;

    Decision(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
