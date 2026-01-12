package no.nav.sak.infrastruktur;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.util.List;

import static org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher.withDefaults;

public abstract class SakOncePerRequestFilter extends OncePerRequestFilter {

	private static final List<RequestMatcher> excludePaths = List.of(
			withDefaults().matcher("/actuator/**"),
			withDefaults().matcher("/api/openapi.json/**"),
			withDefaults().matcher("/v3/api-docs/**"),
			withDefaults().matcher("/swagger-ui/**")
	);

	@Override
	public final boolean shouldNotFilter(HttpServletRequest request) {
		return excludePaths.stream().anyMatch(path -> path.matches(request));
	}

}
