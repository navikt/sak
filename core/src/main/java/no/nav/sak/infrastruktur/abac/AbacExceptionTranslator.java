package no.nav.sak.infrastruktur.abac;

import io.github.resilience4j.circuitbreaker.CircuitBreakerOpenException;
import no.nav.sak.infrastruktur.rest.ExternalApiException;
import no.nav.sak.infrastruktur.rest.ServiceUnavailableException;
import org.apache.commons.lang3.Validate;

import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

class AbacExceptionTranslator {
    private AbacExceptionTranslator() {
    } //Static

    static RuntimeException
    identifyException(Throwable t) {
        if (t instanceof CircuitBreakerOpenException || t instanceof TimeoutException) {
            return new ServiceUnavailableException(t.getMessage(), t);
        } else if (t instanceof ExecutionException) {
            Validate.noNullElements(new Throwable[]{t.getCause(), t.getCause().getCause()});
            Throwable cause = ((ExecutionException) t).getCause().getCause();
            if (cause instanceof SocketTimeoutException) {
                return new ServiceUnavailableException(cause.getMessage(), cause);
            }
        }
        return new ExternalApiException(t.getMessage(), t);
    }
}
