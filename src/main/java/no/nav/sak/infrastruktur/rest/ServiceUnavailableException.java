package no.nav.sak.infrastruktur.rest;

public class ServiceUnavailableException extends RuntimeException {
    public ServiceUnavailableException(String s) {
        super(s);
    }

    public ServiceUnavailableException(String s, Throwable e) {
        super(s, e);
    }
}
