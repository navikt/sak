package no.nav.sak.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.util.ArrayList;
import java.util.List;

public class CustomSakRepositoryImpl implements CustomSakRepository {

	private final EntityManager entityManager;

	public CustomSakRepositoryImpl(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	@Override
	public List<Sak> finnSaker(SakSearchCriteria sakSearchCriteria) {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Sak> cq = cb.createQuery(Sak.class);
		Root<Sak> sak = cq.from(Sak.class);
		List<Predicate> predicates = new ArrayList<>();

		if (sakSearchCriteria.getAktoerId() != null && !sakSearchCriteria.getAktoerId().isEmpty()) {
			predicates.add(sak.get("aktoerId").in(sakSearchCriteria.getAktoerId()));
		}
		sakSearchCriteria.getOrgnr().ifPresent(orgnr -> predicates.add(cb.equal(sak.get("orgnr"), orgnr)));
		sakSearchCriteria.getApplikasjon().ifPresent(applikasjon -> predicates.add(cb.equal(sak.get("applikasjon"), applikasjon)));

		if(!sakSearchCriteria.getTema().isEmpty()) {
			predicates.add(sak.get("tema").in(sakSearchCriteria.getTema()));
		}
		sakSearchCriteria.getFagsakNr().ifPresent(fagsaknr -> predicates.add(cb.equal(sak.get("fagsakNr"), fagsaknr)));

		cq.where(predicates.toArray(new Predicate[0]));
		cq.orderBy(cb.desc(sak.get("opprettetTidspunkt")));

		return entityManager.createQuery(cq).getResultList();
	}
}
