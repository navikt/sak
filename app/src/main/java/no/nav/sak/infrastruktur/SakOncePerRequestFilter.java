package no.nav.sak.infrastruktur;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.util.List;

public abstract class SakOncePerRequestFilter extends OncePerRequestFilter {

	private static final List<RequestMatcher> excludePaths = List.of(new AntPathRequestMatcher("/actuator/**"), new AntPathRequestMatcher("/internal/**"));
	@Override
	public final boolean shouldNotFilter(HttpServletRequest request) {
		return excludePaths.stream().anyMatch(path -> path.matches(request));
	}

}
