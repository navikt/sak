package no.nav.sak.infrastruktur.rest;

public class ExternalApiException extends RuntimeException {
    public ExternalApiException(String s, Exception e) {
        super(s, e);
    }

    public ExternalApiException(String s, Throwable t) {
        super(s, t);
    }

    public ExternalApiException(String s) {
        super(s);
    }
}
