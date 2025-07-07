package no.nav.sak.infrastruktur.abac;

import jakarta.servlet.http.HttpServletRequest;
import no.nav.abac.xacml.NavAttributter;
import no.nav.abac.xacml.StandardAttributter;
import no.nav.resilience.ResilienceConfig;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static no.nav.abac.xacml.NavAttributter.ENVIRONMENT_FELLES_OIDC_TOKEN_BODY;
import static no.nav.abac.xacml.NavAttributter.ENVIRONMENT_FELLES_PEP_ID;
import static no.nav.abac.xacml.NavAttributter.RESOURCE_FELLES_DOMENE;
import static no.nav.abac.xacml.NavAttributter.RESOURCE_FELLES_PERSON_AKTOERID_RESOURCE;
import static no.nav.abac.xacml.NavAttributter.RESOURCE_FELLES_RESOURCE_TYPE;
import static no.nav.sak.infrastruktur.SubjectType.SUBJECT_TYPE_SYSTEMBRUKER;
import static no.nav.sak.infrastruktur.abac.SakPEP.RESOURCE_TYPE_SAK;
import static no.nav.sak.infrastruktur.authentication.AuthenticationFilter.REQUEST_USERNAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SakPEPTest {
    private ABACClient abacClient = mock(ABACClient.class);
    private SakPEP sakPEP = new SakPEP(abacClient, ResilienceConfig.ofDefaults());
    private String username = RandomStringUtils.randomAlphabetic(5);


    @Test
    void autoriserer_for_basic() {
        HttpServletRequest ctx = mockContextFor(username, "Basic 123");

        ABACRequest req = authorizeReturningCaptureOfRequest(ctx, new AuthorizationRequest(null));

        assertDefaulValuesSpecifiedCorrectly(req);
        assertThat(req.getAccessSubject().getAttributes()).containsOnly(
                new ABACAttribute(StandardAttributter.SUBJECT_ID, username),
                new ABACAttribute(NavAttributter.SUBJECT_FELLES_SUBJECTTYPE, SUBJECT_TYPE_SYSTEMBRUKER.getValue()));
    }

    @Test
    void autoriserer_for_oidc() {
        HttpServletRequest ctx = mockContextFor(username, "Bearer 123.321.123");

        ABACRequest req = authorizeReturningCaptureOfRequest(ctx, new AuthorizationRequest(null));

        assertDefaulValuesSpecifiedCorrectly(req);
        assertThat(req.getEnvironment().getAttributes()).contains(new ABACAttribute(ENVIRONMENT_FELLES_OIDC_TOKEN_BODY, "321"));
    }

    @Test
    void autoriserer_med_aktoerid_om_angitt() {
        String aktoerId = "123123123";
        HttpServletRequest ctx = mockContextFor(username, "Bearer 123.321.123");

        ABACRequest req = authorizeReturningCaptureOfRequest(ctx, new AuthorizationRequest(aktoerId));

        assertDefaulValuesSpecifiedCorrectly(req);
        assertThat(req.getResources().get(0).getAttributes()).contains(new ABACAttribute(RESOURCE_FELLES_PERSON_AKTOERID_RESOURCE, aktoerId));
    }

    private HttpServletRequest mockContextFor(String username, String authHeaderContent) {
        HttpServletRequest ctx = mock(HttpServletRequest.class);
        when(ctx.getAttribute(REQUEST_USERNAME)).thenReturn(username);
        when(ctx.getHeader("Authorization")).thenReturn(authHeaderContent);
        return ctx;
    }

    private ABACRequest authorizeReturningCaptureOfRequest(HttpServletRequest ctx, AuthorizationRequest authorizationRequest) {
        when(abacClient.execute(Mockito.any(ABACRequest.class))).thenReturn(mock(ABACResult.class));

        sakPEP.autoriser(ctx, authorizationRequest);

        ArgumentCaptor<ABACRequest> captor = ArgumentCaptor.forClass(ABACRequest.class);
        verify(abacClient).execute(captor.capture());
        return captor.getValue();
    }

    private void assertDefaulValuesSpecifiedCorrectly(ABACRequest req) {
        assertThat(req.getEnvironment().getAttributes()).contains(new ABACAttribute(ENVIRONMENT_FELLES_PEP_ID ,"sak"));
        assertThat(req.getResources().get(0).getAttributes()).contains(
                new ABACAttribute(RESOURCE_FELLES_DOMENE, "sak"),
                new ABACAttribute(RESOURCE_FELLES_RESOURCE_TYPE, RESOURCE_TYPE_SAK));
    }
}
