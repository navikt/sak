package no.nav.sak;

import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

public class OpenApiShimResource extends OpenApiResource {

	@Path("/swagger-config")
	@Produces(MediaType.APPLICATION_JSON)
	@GET
	@Operation(hidden = true)
	public Response fakeConfig() {
		return Response.ok("""
						{
						    "configUrl": "/api/openapi.json/config",
						    "oauth2RedirectUrl": "/swagger-ui/oauth2-redirect.html",
						    "operationsSorter": "alpha",
						    "tagsSorter": "alpha",
						    "url": "/api/openapi.json",
						    "validatorUrl": ""
						}""")
				.build();
	}

}
