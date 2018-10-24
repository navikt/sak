package no.nav.sak;

import no.nav.sak.infrastruktur.abac.ABACJunitClientAlwaysGivingBadAbacMappingToInternalError;
import no.nav.sak.infrastruktur.abac.ABACJunitClientAlwaysGivingBadAbacMappingToServiceUnavailable;
import no.nav.sikkerhet.abac.ABACClient;

public class SakJunitApplicationAlwaysGivingBadAbacMappingToServiceUnavailable extends AbstractSakJunitApplication {

    @Override
    protected ABACClient getABACClient() {
        return ABACJunitClientAlwaysGivingBadAbacMappingToServiceUnavailable.create();
    }
}
