package no.nav.sak;

import no.nav.sak.infrastruktur.abac.ABACJunitClientAlwaysGivingBadAbacMappingToInternalError;
import no.nav.sikkerhet.abac.ABACClient;

public class SakJunitApplicationAlwaysGivingBadAbacMappingToInternalError extends AbstractSakJunitApplication {

    @Override
    protected ABACClient createAbacClient(SakConfiguration sakConfiguration) {
        return ABACJunitClientAlwaysGivingBadAbacMappingToInternalError.create();
    }
}
