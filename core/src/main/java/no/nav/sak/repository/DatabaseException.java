package no.nav.sak.repository;

class DatabaseException extends RuntimeException {
    DatabaseException(Exception e) {
        super(e);
    }
}
