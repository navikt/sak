package no.nav.sak.infrastruktur;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.annotation.Priority;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Priorities;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Component
@Priority(Priorities.AUTHENTICATION - 10)
@Slf4j
public class CorrelationFilter extends SakOncePerRequestFilter {
	private static final String CORRELATION_HEADER = "X-Correlation-ID";
	public static final String MDC_CORRELATION_ID = "correlation-id";
	public static final String UUID_HEADER = "X-UUID";
	public static final String MDC_UUID = "uuid";

	public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	static {
		OBJECT_MAPPER.enable(SerializationFeature.WRAP_ROOT_VALUE);
	}

	@Override
	public void doFilterInternal(HttpServletRequest httpRequest, HttpServletResponse httpResponse, FilterChain filterChain) throws IOException, ServletException {
		try {
			String correlationId = httpRequest.getHeader(CORRELATION_HEADER);
			MDC.put(MDC_UUID, UUID.randomUUID().toString());

			if (StringUtils.isBlank(correlationId)) {
				log.warn("Forventet følgende header: {}, avbryter forespørsel", CORRELATION_HEADER);
				httpResponse.setStatus(BAD_REQUEST.value());
				ErrorResponse body = new ErrorResponse(MDC.get(MDC_UUID), String.format("Påkrevd header mangler: %s", CORRELATION_HEADER));
				httpResponse.getWriter().write(OBJECT_MAPPER.writeValueAsString(body));
				MDC.clear();
				return;
			}

			MDC.put(MDC_CORRELATION_ID, correlationId);

			filterChain.doFilter(httpRequest, httpResponse);

			httpResponse.addHeader(CORRELATION_HEADER, MDC.get(MDC_CORRELATION_ID));
			httpResponse.addHeader(UUID_HEADER, MDC.get(MDC_UUID));
		} finally {
			MDC.clear();
		}
	}

}
