package no.nav.sak.infrastruktur;

import jakarta.annotation.Priority;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Priorities;
import no.nav.sak.infrastruktur.rest.SakRuntimeException;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
@Priority(Priorities.AUTHENTICATION - 10)
public class RequestIdFilter extends SakOncePerRequestFilter {
	public static final String UUID_HEADER = "X-UUID";
	public static final String MDC_UUID = "uuid";

	@Override
	public void doFilterInternal(HttpServletRequest httpRequest, HttpServletResponse httpResponse, FilterChain filterChain) throws IOException, ServletException {
		try {
			MDC.put(MDC_UUID, UUID.randomUUID().toString());

			filterChain.doFilter(httpRequest, httpResponse);

			httpResponse.addHeader(UUID_HEADER, MDC.get(MDC_UUID));
		} catch (RuntimeException unexpectedException) {
			throw new SakRuntimeException(MDC.get(MDC_UUID), "Unexpected exception encountered: " + unexpectedException, unexpectedException);
		} finally {
			MDC.clear();
		}
	}

}
