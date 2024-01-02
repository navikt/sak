package no.nav.sak.infrastruktur;

import io.prometheus.client.Histogram;
import no.nav.sak.infrastruktur.authentication.AuthenticationFilter;
import org.apache.commons.lang3.StringUtils;


import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@EnableApiFilters
public class PrometheusFilter implements ContainerRequestFilter, ContainerResponseFilter {
    private static final Histogram requestsHistogram = Histogram.build("requests_duration_seconds", "Request duration in seconds")
        .labelNames("path", "queryparams", "method", "consumer")
        .register();

    private static final String PROMETHEUS_TIMER = "prometheus_timer";

    @Override
    public void filter(ContainerRequestContext containerRequestContext) throws IOException {
        UriInfo uriInfo = containerRequestContext.getUriInfo();
        String path = uriInfo.getRequestUri().getPath();
        MultivaluedMap<String, String> pathParameters = uriInfo.getPathParameters();

        String sanitizedPath;
        String jerseyApiPrefix = "/api/v1";
        if (path.startsWith(jerseyApiPrefix)) {
            sanitizedPath = jerseyApiPrefix + replacePathparams(StringUtils.remove(path, jerseyApiPrefix), pathParameters);
        } else {
            sanitizedPath = path;
        }
        String queryparams = "N/A";
        if (!uriInfo.getQueryParameters().isEmpty()) {
            queryparams = uriInfo.getQueryParameters().keySet().toString();
        }

        Histogram.Timer timer = requestsHistogram
            .labels(
                sanitizedPath,
                queryparams,
                containerRequestContext.getMethod(),
                (String) containerRequestContext.getProperty(AuthenticationFilter.REQUEST_CONSUMERID)).startTimer();
        containerRequestContext.setProperty(PROMETHEUS_TIMER, timer);
    }

    private String replacePathparams(String path, MultivaluedMap<String, String> pathParameters) {
        String modifiedPath = path;
        for (Map.Entry<String, List<String>> entry : pathParameters.entrySet()) {
            String originalPathFragment = String.format("{%s}", entry.getKey());
            modifiedPath = StringUtils.replaceEach(path, entry.getValue().toArray(new String[0]), new String[]{originalPathFragment});
        }

        return modifiedPath;
    }

    @Override
    public void filter(ContainerRequestContext containerRequestContext, ContainerResponseContext containerResponseContext) throws IOException {
        Histogram.Timer timer = Histogram.Timer.class.cast(containerRequestContext.getProperty(PROMETHEUS_TIMER));
        if (timer != null) {
            timer.observeDuration();
        }


    }
}
