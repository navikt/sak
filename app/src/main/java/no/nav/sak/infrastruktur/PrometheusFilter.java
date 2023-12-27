package no.nav.sak.infrastruktur;

import io.prometheus.client.Histogram;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import no.nav.sak.infrastruktur.authentication.AuthenticationFilter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.util.Map;

@Component
public class PrometheusFilter implements Filter {
    private static final Histogram requestsHistogram = Histogram.build("requests_duration_seconds", "Request duration in seconds")
        .labelNames("path", "queryparams", "method", "consumer")
        .register();

    private static final String PROMETHEUS_TIMER = "prometheus_timer";

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
		Histogram.Timer timer = null;
		if (servletRequest instanceof HttpServletRequest httpRequest) {
			String path = httpRequest.getRequestURI();// .getPath();
			Map<String, String> pathParameters = (Map<String, String>) httpRequest.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);

			String sanitizedPath;
			String jerseyApiPrefix = "/api/v1";
			if (path.startsWith(jerseyApiPrefix)) {
				sanitizedPath = jerseyApiPrefix + replacePathparams(StringUtils.remove(path, jerseyApiPrefix), pathParameters);
			} else {
				sanitizedPath = path;
			}
			String queryparams = "N/A";
			if (httpRequest.getQueryString() != null && !httpRequest.getQueryString().isEmpty()) {
				queryparams = httpRequest.getQueryString();
			}

			timer = requestsHistogram
					.labels(
							sanitizedPath,
							queryparams,
							httpRequest.getMethod(),
							(String) httpRequest.getAttribute(AuthenticationFilter.REQUEST_CONSUMERID))
					.startTimer();
		}

		filterChain.doFilter(servletRequest, servletResponse);

		if (timer != null) {
			timer.observeDuration();
		}
    }

    private static String replacePathparams(String path, Map<String, String> pathParameters) {
        String modifiedPath = path;
		if (pathParameters != null)
        for (Map.Entry<String, String> entry : pathParameters.entrySet()) {
            String originalPathFragment = String.format("{%s}", entry.getKey());
            modifiedPath = StringUtils.replace(path, entry.getValue(), originalPathFragment);
        }

        return modifiedPath;
    }

}
