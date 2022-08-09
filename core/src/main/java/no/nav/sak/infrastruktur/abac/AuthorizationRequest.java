package no.nav.sak.infrastruktur.abac;

import java.util.Optional;

public class AuthorizationRequest {
    private String aktoerId;

    public AuthorizationRequest(String aktoerId) {
        this.aktoerId = aktoerId;
    }

    public Optional<String> getAktoerId() {
        return Optional.ofNullable(aktoerId);
    }

}
