package no.nav.sak.app.jersey;

import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import org.glassfish.jersey.server.ResourceConfig;

public class SakSwaggerConfig extends ResourceConfig {

	public SakSwaggerConfig() {
		packages("no.nav.sak.app");
		register(OpenApiResource.class);
	}
}
