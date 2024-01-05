package no.nav.sak.infrastruktur.abac;

import no.nav.resilience.ResilienceConfig;

public class MockableSakPEP extends SakPEP {
	public MockableSakPEP(ABACClient abacClient, ResilienceConfig resilienceConfig) {
		super(abacClient, resilienceConfig);
	}

	public ABACResult executeAbacRequest(ABACRequest abacRequest) {
		return super.executeAbacRequest(abacRequest);
	}
}
