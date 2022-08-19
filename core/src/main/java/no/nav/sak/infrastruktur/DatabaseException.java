package no.nav.sak.infrastruktur;

class DatabaseException extends RuntimeException {
    DatabaseException(Exception e) {
        super(e);
    }
}
