package no.nav.sak.infrastruktur.abac;

import no.nav.sikkerhet.abac.ABACClient;
import no.nav.sikkerhet.abac.ABACRequest;
import no.nav.sikkerhet.abac.ABACResult;
import org.mockito.Mockito;

import java.util.*;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ABACJunitClientAlwaysGivingBadAbacMappingToInternalError {

    private static final Set<ABACResult.Code> abacResultCodesMappingToInternalError;
    static {
        final List<ABACResult.Code> tempAbacResultCodeList = Arrays.asList(ABACResult.Code.values());
        Collections.shuffle(tempAbacResultCodeList, new Random(197));
        final Set<ABACResult.Code> tempAbacResultCodeSet = new HashSet<ABACResult.Code>(tempAbacResultCodeList);
        tempAbacResultCodeSet.remove(ABACResult.Code.OK);
        //tempAbacResultCodeSet.remove(ABACResult.Code.INVALID);

        abacResultCodesMappingToInternalError = Collections.unmodifiableSet(tempAbacResultCodeSet);
    }

    public static ABACClient create() {

        final ABACClient abacClient = mock(ABACClient.class);
        final ABACResult abacResult = mock(ABACResult.class);
        final ABACResult.Code[] arrayOfAbacResultCodesExcludedOk = abacResultCodesMappingToInternalError.toArray(ABACResult.Code[]::new);
        when(abacResult.getResultCode()).thenReturn(arrayOfAbacResultCodesExcludedOk[0], arrayOfAbacResultCodesExcludedOk);
        when(abacClient.execute(Mockito.any(ABACRequest.class))).thenReturn(abacResult);
        when(abacResult.hasAccess()).thenReturn(false);

        return abacClient;
    }
}
