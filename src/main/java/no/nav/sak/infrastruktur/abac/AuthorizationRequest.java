package no.nav.sak.infrastruktur.abac;

import java.util.Optional;

public class AuthorizationRequest {
    private String aktoerId;
    private String tema;

    public AuthorizationRequest(String aktoerId) {
        this.aktoerId = aktoerId;
    }

    public AuthorizationRequest(String aktoerId, String tema) {
        this.aktoerId = aktoerId;
        this.tema = tema;
    }

    public Optional<String> getAktoerId() {
        return Optional.ofNullable(aktoerId);
    }

    public Optional<String> getTema() {
        return Optional.ofNullable(tema);
    }
}
