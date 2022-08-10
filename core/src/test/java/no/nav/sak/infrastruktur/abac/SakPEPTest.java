package no.nav.sak.infrastruktur.abac;

import no.nav.abac.xacml.NavAttributter;
import no.nav.abac.xacml.StandardAttributter;
import no.nav.resilience.ResilienceConfig;
import no.nav.sikkerhet.abac.ABACAttribute;
import no.nav.sikkerhet.abac.ABACClient;
import no.nav.sikkerhet.abac.ABACRequest;
import no.nav.sikkerhet.abac.ABACResult;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.UriInfo;

import static no.nav.abac.xacml.NavAttributter.*;
import static no.nav.sak.infrastruktur.SubjectType.SUBJECT_TYPE_SYSTEMBRUKER;
import static no.nav.sak.infrastruktur.abac.SakPEP.RESOURCE_TYPE_SAK;
import static no.nav.sak.infrastruktur.authentication.AuthenticationFilter.REQUEST_USERNAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SakPEPTest {
    private ABACClient abacClient = mock(ABACClient.class);
    private SakPEP sakPEP = new SakPEP(abacClient, ResilienceConfig.ofDefaults());
    private String username = RandomStringUtils.randomAlphabetic(5);


    @Test
    void autoriserer_for_basic() {
        ContainerRequestContext ctx = mockContextFor(username, "Basic 123");

        ABACRequest req = authorizeReturningCaptureOfRequest(ctx, new AuthorizationRequest(null));

        assertDefaulValuesSpecifiedCorrectly(req);
        assertThat(req.getAccessSubject().getAttributes()).containsOnly(
            new ABACAttribute(StandardAttributter.SUBJECT_ID, username),
            new ABACAttribute(NavAttributter.SUBJECT_FELLES_SUBJECTTYPE, SUBJECT_TYPE_SYSTEMBRUKER.getValue()));
    }

    @Test
    void autoriserer_for_saml() {
        ContainerRequestContext ctx = mockContextFor(username, "Saml 123");

        ABACRequest req = authorizeReturningCaptureOfRequest(ctx, new AuthorizationRequest(null));

        assertDefaulValuesSpecifiedCorrectly(req);
        assertThat(req.getEnvironment().getAttributes()).contains(new ABACAttribute(ENVIRONMENT_FELLES_SAML_TOKEN, "123"));
    }

    @Test
    void autoriserer_for_oidc() {
        ContainerRequestContext ctx = mockContextFor(username, "Bearer 123.321.123");

        ABACRequest req = authorizeReturningCaptureOfRequest(ctx, new AuthorizationRequest(null));

        assertDefaulValuesSpecifiedCorrectly(req);
        assertThat(req.getEnvironment().getAttributes()).contains(new ABACAttribute(ENVIRONMENT_FELLES_OIDC_TOKEN_BODY, "321"));
    }

    @Test
    void autoriserer_med_aktoerid_om_angitt() {
        String aktoerId = "123123123";
        ContainerRequestContext ctx = mockContextFor(username, "Bearer 123.321.123");

        ABACRequest req = authorizeReturningCaptureOfRequest(ctx, new AuthorizationRequest(aktoerId));

        assertDefaulValuesSpecifiedCorrectly(req);
        assertThat(req.getResources().get(0).getAttributes()).contains(new ABACAttribute(RESOURCE_FELLES_PERSON_AKTOERID_RESOURCE, aktoerId));
    }

    private ContainerRequestContext mockContextFor(String username, String authHeaderContent) {
        ContainerRequestContext ctx = mock(ContainerRequestContext.class);
        when(ctx.getProperty(REQUEST_USERNAME)).thenReturn(username);
        when(ctx.getHeaderString("Authorization")).thenReturn(authHeaderContent);
            when(ctx.getUriInfo()).thenReturn(mock(UriInfo.class));
        when(ctx.getRequest()).thenReturn(mock(Request.class));
        return ctx;
    }

    private ABACRequest authorizeReturningCaptureOfRequest(ContainerRequestContext ctx, AuthorizationRequest authorizationRequest) {
        when(abacClient.execute(Mockito.any(ABACRequest.class))).thenReturn(mock(ABACResult.class));

        sakPEP.autoriser(ctx, authorizationRequest);

        ArgumentCaptor<ABACRequest> captor = ArgumentCaptor.forClass(ABACRequest.class);
        verify(abacClient).execute(captor.capture());
        return captor.getValue();
    }

    private void assertDefaulValuesSpecifiedCorrectly(ABACRequest req) {
        assertThat(req.getEnvironment().getAttributes()).contains(new ABACAttribute(ENVIRONMENT_FELLES_PEP_ID , "no/nav/sak"));
        assertThat(req.getResources().get(0).getAttributes()).contains(
            new ABACAttribute(RESOURCE_FELLES_DOMENE, "no/nav/sak"),
            new ABACAttribute(RESOURCE_FELLES_RESOURCE_TYPE, RESOURCE_TYPE_SAK));
    }
}
