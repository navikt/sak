package no.nav.sak.infrastruktur.abac;

import no.nav.sikkerhet.abac.ABACClient;
import no.nav.sikkerhet.abac.ABACRequest;
import no.nav.sikkerhet.abac.ABACResult;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ABACJunitClientAlwaysGivingBadAbac {

    private static final Set<ABACResult.Code> abacResultCodesExcludedOk;
    static {
        final Set<ABACResult.Code> tempAbacResultCodes = new HashSet<ABACResult.Code>();
        tempAbacResultCodes.addAll(Arrays.asList(ABACResult.Code.values()));
        tempAbacResultCodes.remove(ABACResult.Code.OK);
        abacResultCodesExcludedOk = Collections.unmodifiableSet(tempAbacResultCodes);
    }

    public static ABACClient create() {

        final ABACClient abacClient = mock(ABACClient.class);
        final ABACResult abacResult = mock(ABACResult.class);
        final ABACResult.Code[] arrayOfAbacResultCodesExcludedOk = new ABACResult.Code[abacResultCodesExcludedOk.size()];

        final Iterator<ABACResult.Code> it = abacResultCodesExcludedOk.iterator();
        for (int i = 0; i < abacResultCodesExcludedOk.size(); i++) {
            arrayOfAbacResultCodesExcludedOk[i++] = it.next();
        }
        when(abacResult.getCode()).thenReturn(arrayOfAbacResultCodesExcludedOk[0], arrayOfAbacResultCodesExcludedOk);
        when(abacClient.execute(Mockito.any(ABACRequest.class))).thenReturn(abacResult);
        when(abacResult.hasAccess()).thenReturn(false);

        return abacClient;
    }
}
