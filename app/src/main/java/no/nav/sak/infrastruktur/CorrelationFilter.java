package no.nav.sak.infrastruktur;

import jakarta.annotation.Priority;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.UUID;

@Component
// @Provider
@Priority(Priorities.HEADER_DECORATOR)
@Slf4j
public class CorrelationFilter implements Filter {
    private static final String CORRELATION_HEADER = "X-Correlation-ID";

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
		try {
			if (servletRequest instanceof HttpServletRequest httpRequest) {
				String correlationId = httpRequest.getHeader(CORRELATION_HEADER);
				MDC.put("uuid", UUID.randomUUID().toString());

				if (StringUtils.isBlank(correlationId)) {
					log.warn("Forventet følgende header: {}, avbryter forespørsel", CORRELATION_HEADER);
					throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
							new ErrorResponse(MDC.get("uuid"), String.format("Påkrevd header mangler: %s", CORRELATION_HEADER)).toString());
				}

				MDC.put("correlation-id", correlationId);
			}

			filterChain.doFilter(servletRequest, servletResponse);

			if (servletResponse instanceof HttpServletResponse httpResponse) {
				httpResponse.addHeader(CORRELATION_HEADER, MDC.get("correlation-id"));
				httpResponse.addHeader("X-UUID", MDC.get("uuid"));
			}
		} finally {
			MDC.clear();
		}
	}

}
