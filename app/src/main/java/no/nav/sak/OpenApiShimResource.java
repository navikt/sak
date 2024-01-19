package no.nav.sak;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

@Path("/openapi.{type:json|yaml}")
public class OpenApiShimResource {

	@Path("/swagger-config")
	@Produces(MediaType.APPLICATION_JSON)
	@GET
	@Operation(hidden = true)
	public Response fakeConfig() {
		return Response.ok("""
						{
						    "configUrl": "/api/openapi.json/swagger-config",
						    "oauth2RedirectUrl": "/swagger-ui/oauth2-redirect.html",
						    "operationsSorter": "alpha",
						    "tagsSorter": "alpha",
						    "url": "/api/openapi.json",
						    "validatorUrl": ""
						}""")
				.build();
	}

	@GET
	@Produces({"application/json"})
	@Operation(hidden = true)
	public Response getOpenApi(@Context HttpHeaders headers, @Context UriInfo uriInfo, @PathParam("type") String type) throws Exception {
		try (var openApiStream =  this.getClass().getResourceAsStream("/openapi.json")) {
			return Response.ok(openApiStream.readAllBytes()).build();
		}
	}

}
