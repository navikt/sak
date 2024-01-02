package no.nav.sak.infrastruktur.abac;

import no.nav.resilience.ResilienceConfig;
import no.nav.sikkerhet.abac.ABACClient;
import no.nav.sikkerhet.abac.ABACRequest;
import no.nav.sikkerhet.abac.ABACResult;

public class MockableSakPEP extends SakPEP {
	public MockableSakPEP(ABACClient abacClient, ResilienceConfig resilienceConfig) {
		super(abacClient, resilienceConfig);
	}

	public ABACResult executeAbacRequest(ABACRequest abacRequest) {
		return super.executeAbacRequest(abacRequest);
	}
}
