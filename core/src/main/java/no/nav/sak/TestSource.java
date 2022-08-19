package no.nav.sak;


import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

@Path("test")
public class TestSource {


	@GET
	public String getGreeting(@QueryParam("name") String name) {
		return name;
	}
}
