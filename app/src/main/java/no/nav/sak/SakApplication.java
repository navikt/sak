package no.nav.sak;

import io.prometheus.client.hotspot.DefaultExports;
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import lombok.extern.slf4j.Slf4j;
import no.nav.sak.infrastruktur.CorrelationFilter;
import no.nav.sak.infrastruktur.DefaultExceptionMapper;
import no.nav.sak.infrastruktur.MethodNotAllowedExceptionMapper;
import no.nav.sak.infrastruktur.NotFoundExceptionMapper;
import no.nav.sak.infrastruktur.ParamExceptionMapper;
import no.nav.sak.infrastruktur.PrometheusFilter;
import no.nav.sak.infrastruktur.authentication.AuthenticationFilter;
import no.nav.sak.infrastruktur.rest.ExternalApiExceptionMapper;
import no.nav.sak.infrastruktur.rest.ServiceUnavailableExceptionMapper;
import no.nav.sak.validering.ConstraintValidationExceptionMapper;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.opensaml.core.config.InitializationException;
import org.opensaml.core.config.InitializationService;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.stereotype.Component;

import jakarta.ws.rs.ApplicationPath;
import java.util.logging.Level;

import static java.util.logging.Logger.getLogger;

@ApplicationPath("/api")
@Component
@Slf4j
public class SakApplication extends ResourceConfig {

	public SakApplication() {
		log.info("Initializing SAK Jersey application");
		DefaultExports.initialize();

		register(SakResource.class);
		registerFilters();
		registerExceptionmappers();
		register(new JacksonFeature());
		registerLoggingFeature();
		initSAML();
		register(OpenApiResource.class);
		property(ServerProperties.WADL_FEATURE_DISABLE, "true");

		log.info("Jersey-Application ferdig initialisert");
	}

	private void registerExceptionmappers() {
		register(new ConstraintValidationExceptionMapper());
		register(new DefaultExceptionMapper());
		register(new NotFoundExceptionMapper());
		register(new MethodNotAllowedExceptionMapper());
		register(new ParamExceptionMapper());
		register(new ExternalApiExceptionMapper());
		register(new ServiceUnavailableExceptionMapper());
	}

	private void registerFilters() {
		register(new CorrelationFilter());
		register(new PrometheusFilter());
		register(AuthenticationFilter.class);
	}

	private void initSAML() {
		try {
			InitializationService.initialize();
		} catch (InitializationException e) {
			throw new IllegalStateException("Feilet under initialisering av SAML", e);
		}
	}

	private void registerLoggingFeature() {
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();
		register(new LoggingFeature(getLogger(LoggingFeature.class.getName()), Level.INFO, LoggingFeature.Verbosity.PAYLOAD_TEXT, LoggingFeature.DEFAULT_MAX_ENTITY_SIZE));
	}
}
