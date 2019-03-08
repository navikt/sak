package no.nav.sak;

import no.nav.sak.infrastruktur.abac.ABACJunitClient;
import no.nav.sikkerhet.abac.ABACClient;

public class SakJunitApplication extends AbstractSakJunitApplication {

    @Override
    protected ABACClient createAbacClient(SakConfiguration sakConfiguration) {
        return ABACJunitClient.create();
    }
}
