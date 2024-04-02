package no.nav.sak.infrastruktur.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.annotation.Priority;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Priorities;
import lombok.extern.slf4j.Slf4j;
import no.nav.sak.infrastruktur.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Component
@Priority(Priorities.AUTHENTICATION - 1)
@Slf4j
public class UnauthorizedExceptionHandlingFilter implements Filter {

	public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	static {
		OBJECT_MAPPER.enable(SerializationFeature.WRAP_ROOT_VALUE);
	}

	private final SakRestExceptionHandler sakRestExceptionHandler;

	public UnauthorizedExceptionHandlingFilter(SakRestExceptionHandler sakRestExceptionHandler) {
		this.sakRestExceptionHandler = sakRestExceptionHandler;
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
		try {
			filterChain.doFilter(servletRequest, servletResponse);
		} catch (UnauthorizedException unauthorizedException) {
			if (servletResponse instanceof HttpServletResponse httpServletResponse) {
				httpServletResponse.setStatus(UNAUTHORIZED.value());
				String s = OBJECT_MAPPER.writeValueAsString(unauthorizedException.getErrorResponse());
				httpServletResponse.getWriter().write(s);

				String path = servletRequest instanceof HttpServletRequest httpServletRequest ? httpServletRequest.getRequestURI() : "<unknown path>";
				log.warn("sak request unauthorized path={} correlationId={} uuid={}", path,
						unauthorizedException.getCorrelationId(), unauthorizedException.getUuid(), unauthorizedException);
			} else {
				log.error("sak request unauthorized (not http request)", unauthorizedException);
			}
		} catch (CorrelatableSakRuntimeException exception) {
			String path = servletRequest instanceof HttpServletRequest httpServletRequest ? httpServletRequest.getRequestURI() : "<unknown path>";
			log.error("sak encountered an unexpected exception when handling request path={} correlationId={} uuid={}",
					path, exception.getCorrelationId(), exception.getUuid(), exception);
		} catch (RuntimeException unexpectedRuntimeException)  {
			String path = servletRequest instanceof HttpServletRequest httpServletRequest ? httpServletRequest.getRequestURI() : "<unknown path>";
			log.error("sak encountered an unexpected exception when handling request @ {}", path, unexpectedRuntimeException);
		}

	}
}
