package no.nav.sak.infrastruktur.abac;

import org.mockito.Mockito;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ABACJunitClient {

    public static ABACClient create() {

        final ABACClient abacClient = mock(ABACClient.class);
        final ABACResult abacResult = mock(ABACResult.class);
        when(abacResult.getResultCode()).thenReturn(ABACResult.Code.OK);
        when(abacClient.execute(Mockito.any(ABACRequest.class))).thenReturn(abacResult);
        when(abacResult.hasAccess()).thenReturn(true);

        return abacClient;
    }
}
