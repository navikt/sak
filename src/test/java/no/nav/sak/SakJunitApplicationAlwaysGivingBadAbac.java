package no.nav.sak;

import no.nav.sak.infrastruktur.abac.ABACJunitClientAlwaysGivingBadAbac;
import no.nav.sikkerhet.abac.ABACClient;

public class SakJunitApplicationAlwaysGivingBadAbac extends AbstractSakJunitApplication {

    @Override
    protected ABACClient getABACClient() {
        return ABACJunitClientAlwaysGivingBadAbac.create();
    }
}
