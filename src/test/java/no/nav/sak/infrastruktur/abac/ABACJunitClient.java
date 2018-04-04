package no.nav.sak.infrastruktur.abac;

import no.nav.sikkerhet.abac.ABACClient;
import no.nav.sikkerhet.abac.ABACRequest;
import no.nav.sikkerhet.abac.ABACResult;
import org.mockito.Mockito;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ABACJunitClient {
    public static ABACClient create() {
        ABACClient abacClient = mock(ABACClient.class);
        ABACResult abacResult = mock(ABACResult.class);
        when(abacClient.execute(Mockito.any(ABACRequest.class))).thenReturn(abacResult);
        when(abacResult.hasAccess()).thenReturn(true);
        return abacClient;
    }
}
