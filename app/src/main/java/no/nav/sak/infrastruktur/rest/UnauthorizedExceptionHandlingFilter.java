package no.nav.sak.infrastruktur.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.annotation.Priority;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Priorities;
import lombok.extern.slf4j.Slf4j;
import no.nav.sak.infrastruktur.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.io.IOException;

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
				ResponseEntity<ErrorResponse> errorResponse = sakRestExceptionHandler.unauthorizedExceptionMapper(unauthorizedException);

				httpServletResponse.setStatus(errorResponse.getStatusCode().value());
				ErrorResponse body = errorResponse.getBody();
				String s = OBJECT_MAPPER.writeValueAsString(body);
				httpServletResponse.getWriter().write(s);
			}
		}

	}
}
