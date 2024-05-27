package no.nav.sak.repository;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.*;

@Repository
public class SakRepository {
    private final Database database;

    public SakRepository(Database database) {
        this.database = database;
    }

    public Sak lagre(Sak sak) {
		Long id = database.insert("insert into sak (id, aktoerid, orgnr, tema, applikasjon, fagsaknr, opprettet_av, opprettet_tidspunkt)" +
						" values (seq_sak.nextval, ?, ?, ?, ?, ?, ?, ?)",
				sak.getAktoerId(),
				sak.getOrgnr(),
				sak.getTema(),
				sak.getApplikasjon(),
				sak.getFagsakNr(),
				sak.getOpprettetAv(),
				Timestamp.valueOf(sak.getOpprettetTidspunkt()));
		sak.setId(id);
        return sak;
    }

    public Optional<Sak> hentSak(Long id) {
		return database.queryForSingle("select * from sak where id = ?", this::toSak, id);
    }

    public List<Sak> finnSaker(SakSearchCriteria sakSearchCriteria) {
        Query query = new Query("select * from sak");
        if (sakSearchCriteria.getAktoerId() != null && !sakSearchCriteria.getAktoerId().isEmpty()) {
            String parameters = sakSearchCriteria.getAktoerId().stream().map(t -> "?").collect(joining(","));
            query.in("aktoerId in (" + parameters + ")", sakSearchCriteria.getAktoerId());
        }
        sakSearchCriteria.getOrgnr().ifPresent(orgnr -> query.and("orgnr = ?", orgnr));
        sakSearchCriteria.getApplikasjon().ifPresent(applikasjon -> query.and("applikasjon = ?", applikasjon));

        if(!sakSearchCriteria.getTema().isEmpty()) {
            String parameters = sakSearchCriteria.getTema().stream().map(t -> "?").collect(joining(","));
            query.in("tema in (" + parameters + ")", sakSearchCriteria.getTema());
        }
        sakSearchCriteria.getFagsakNr().ifPresent(fagsaknr -> query.and("fagsaknr = ?", fagsaknr));
        query.sql.append(" order by opprettet_tidspunkt desc");
		return velgTidligstOpprettetOmDupliserte(database.queryForList(query.sql.toString(), query.params, this::toSak));
	}

	private Sak toSak(Database.Row row) throws SQLException {
        return new Sak.Builder()
            .medId(row.getLong("id"))
            .medAktoerId(row.getString("aktoerId"))
            .medOrgnr(row.getString("orgnr"))
            .medTema(row.getString("tema"))
            .medApplikasjon(row.getString("applikasjon"))
            .medFagsakNr(row.getString("fagsaknr"))
            .medOpprettetAv(row.getString("opprettet_av"))
            .medOpprettetTidspunkt(row.getLocalDateTime("opprettet_tidspunkt"))
            .build();
    }

	private static List<Sak> velgTidligstOpprettetOmDupliserte(List<Sak> saks) {
		return saks
				.stream()
				.collect(groupingBy(Sak::hashForDuplikat,
								minBy(Comparator.nullsLast(Comparator.comparing(Sak::getOpprettetTidspunkt)))))
				.values()
				.stream()
				.map(Optional::get)
				.toList();
	}

	private static class Query {
        private final StringBuilder sql = new StringBuilder();
        private final List<Object> params = new ArrayList<>();

        Query(String sql) {
            this.sql.append(sql);
        }

        void and(String criteria, Object value) {
            addWhereOrAndToSql();
            sql.append(" ").append(criteria);
            for (int i = 0; i < StringUtils.countMatches(criteria, "?"); i++) {
                params.add(value);
            }
        }

        void in(String criteria, List<?> values) {
            addWhereOrAndToSql();
            sql.append(" ").append(criteria);
            params.addAll(values);
        }

        private void addWhereOrAndToSql() {
            sql.append(" ");
            if (params.isEmpty()) {
                sql.append("where");
            } else {
                sql.append("and");
            }
        }
    }
}

