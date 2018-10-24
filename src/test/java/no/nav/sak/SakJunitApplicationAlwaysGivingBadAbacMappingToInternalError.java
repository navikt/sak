package no.nav.sak;

import no.nav.sak.infrastruktur.abac.ABACJunitClientAlwaysGivingBadAbacMappingToInternalError;
import no.nav.sikkerhet.abac.ABACClient;

public class SakJunitApplicationAlwaysGivingBadAbacMappingToInternalError extends AbstractSakJunitApplication {

    @Override
    protected ABACClient getABACClient() {
        return ABACJunitClientAlwaysGivingBadAbacMappingToInternalError.create();
    }
}
