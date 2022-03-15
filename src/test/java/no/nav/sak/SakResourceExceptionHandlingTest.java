package no.nav.sak;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import no.nav.sak.infrastruktur.DefaultExceptionMapper;
import no.nav.sak.infrastruktur.abac.SakPEP;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class SakResourceExceptionHandlingTest extends JerseyTest {

    @BeforeEach
    void before() throws Exception {
        super.setUp();
    }

    @Test
    void returnerer_500_med_uuid_og_aarsak_naar_intern_systemfeil() {
        Response response = target("/v1/saker").path("1").request().get();

        assertThat(response.getStatus()).isEqualTo(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        assertThat(response.getHeaderString("Content-Type")).isEqualTo("application/json");

        JsonObject jsonObject = new JsonParser().parse(new InputStreamReader((ByteArrayInputStream) response.getEntity())).getAsJsonObject();
        assertThat(jsonObject.get("uuid")).isNotNull();
        assertThat(jsonObject.get("feilmelding")).isNotNull();
    }


    @Override
    protected Application configure() {
        ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.register(new DefaultExceptionMapper());

        SakRepository sakRepository = mock(SakRepository.class);
        Mockito.doThrow(new IllegalStateException("Jeg feiler")).when(sakRepository).hentSak(Mockito.anyLong());

        resourceConfig.registerInstances(new SakResource(sakRepository, mock(SakPEP.class)));
        return resourceConfig;
    }
}
