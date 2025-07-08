package no.nav.sak.repository;

import java.util.List;

public interface CustomSakRepository {
	List<Sak> finnSaker(SakSearchCriteria sakSearchCriteria);
}
