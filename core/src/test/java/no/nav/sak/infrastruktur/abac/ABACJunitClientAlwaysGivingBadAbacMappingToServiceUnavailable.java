package no.nav.sak.infrastruktur.abac;

import io.github.resilience4j.circuitbreaker.CircuitBreakerOpenException;
import no.nav.resilience.ResilienceExecutor;
import no.nav.sikkerhet.abac.ABACClient;
import no.nav.sikkerhet.abac.ABACRequest;
import no.nav.sikkerhet.abac.ABACResult;
import org.mockito.Mockito;

import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ABACJunitClientAlwaysGivingBadAbacMappingToServiceUnavailable {

    private static final Set<ABACResult.Code> abacResultCodesMappingToServiceUnavailable;
    static {
        final List<ABACResult.Code> tempAbacResultCodeList = Arrays.asList(ABACResult.Code.values());
        Collections.shuffle(tempAbacResultCodeList, new Random(197));
        final Set<ABACResult.Code> tempAbacResultCodeSet = new HashSet<ABACResult.Code>(tempAbacResultCodeList);

        tempAbacResultCodeSet.remove(ABACResult.Code.OK);
        //tempAbacResultCodeSet.remove(ABACResult.Code.INVALID);

        abacResultCodesMappingToServiceUnavailable = Collections.unmodifiableSet(tempAbacResultCodeSet);
    }

    public static ABACClient create() {

        final ResilienceExecutor resilienceExecutor= mock(ResilienceExecutor.class);

        final ABACClient abacClient = mock(ABACClient.class);
        final ABACResult abacResult = mock(ABACResult.class);
        final ABACResult.Code[] arrayOfAbacResultCodesExcludedOk = new ABACResult.Code[abacResultCodesMappingToServiceUnavailable.size()];

        final Iterator<ABACResult.Code> it = abacResultCodesMappingToServiceUnavailable.iterator();
        for (int i = 0; i < abacResultCodesMappingToServiceUnavailable.size(); i++) {
            arrayOfAbacResultCodesExcludedOk[i++] = it.next();
        }
        when(abacResult.getResultCode()).thenReturn(arrayOfAbacResultCodesExcludedOk[0], arrayOfAbacResultCodesExcludedOk);
        when(abacClient.execute(Mockito.any(ABACRequest.class))).thenThrow(SocketTimeoutException.class);
        when(abacResult.hasAccess()).thenReturn(false);

        when(resilienceExecutor.execute(Mockito.any(ABACRequest.class))).thenThrow(CircuitBreakerOpenException.class);

        return abacClient;
    }
}
