package no.nav.sak;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

@Path("test")
public class TestSource {


	@GET
	public String getGreeting(@QueryParam("name") String name) {
		return name;
	}
}
