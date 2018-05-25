package no.nav.sak;

import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
import no.nav.sak.infrastruktur.Database;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.defaultString;

public class SakRepository {
    private static final Counter opprettedeSakerCounter = Counter.build("saker_opprettet_total", "Antall saker opprettet totalt")
        .labelNames("tema", "type", "applikasjon", "consumer").register();

    private static final Histogram latencyHisto = Histogram.build("repository_duration_seconds", "Repository latency in seconds")
        .labelNames("operation", "consumer")
        .register();

    private final Database database;

    public SakRepository(Database database) {
        this.database = database;
    }

    public Sak lagre(Sak sak) {
        Histogram.Timer timer = startTimer("insert");
        try {
            Long id = database.insert("insert into sak (id, aktoerid, orgnr, tema, applikasjon, fagsaknr, opprettet_av, opprettet_tidspunkt)" +
                    " values (sak_seq.nextval, ?, ?, ?, ?, ?, ?, ?)",
                sak.getAktoerId(),
                sak.getOrgnr(),
                sak.getTema(),
                sak.getApplikasjon(),
                sak.getFagsakNr(),
                sak.getOpprettetAv(),
                Timestamp.valueOf(sak.getOpprettetTidspunkt()));
            sak.setId(id);
        } finally {
            timer.observeDuration();
        }
        opprettedeSakerCounter.labels(
            sak.getTema(),
            sak.getFagsakNr() != null ? "Fagsak" : "Generell",
            sak.getApplikasjon(),
            defaultString(MDC.get("consumerid"), "N/A")).inc();
        return sak;
    }

    Optional<Sak> hentSak(Long id) {
        Histogram.Timer timer = startTimer("get");
        Optional<Sak> result;
        try {
             result = database.queryForSingle("select * from sak where id = ?", this::toSak, id);
        } finally {
            timer.observeDuration();
        }
        return result;
    }

    public List<Sak> finnSaker(SakSearchCriteria sakSearchCriteria) {
        Query query = new Query("select * from sak");
        sakSearchCriteria.getAktoerId().ifPresent(aktoerId -> query.and("aktoerId = ?", aktoerId));
        sakSearchCriteria.getOrgnr().ifPresent(orgnr -> query.and("orgnr = ?", orgnr));
        sakSearchCriteria.getApplikasjon().ifPresent(applikasjon -> query.and("applikasjon = ?", applikasjon));
        sakSearchCriteria.getTema().ifPresent(tema -> query.and("tema = ?", tema));
        sakSearchCriteria.getFagsakNr().ifPresent(fagsaknr -> query.and("fagsaknr = ?", fagsaknr));
        Histogram.Timer timer = startTimer("search");
        List<Sak> result;
        try {
            result = database.queryForList(query.sql.toString(), query.params, this::toSak);
        } finally {
            timer.observeDuration();
        }
        return result;
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

    private Histogram.Timer startTimer(String operation) {
        return latencyHisto
            .labels(
                operation,
                defaultString(MDC.get("consumerid"), "N/A")).startTimer();
    }

    private static class Query {
        private final StringBuilder sql = new StringBuilder();
        private final List<Object> params = new ArrayList<>();

        Query(String sql) {
            this.sql.append(sql);
        }

        void and(String criteria, Object value) {
            sql.append(" ");
            if (params.isEmpty()) {
                sql.append("where");
            } else {
                sql.append("and");
            }
            sql.append(" ").append(criteria);
            for (int i = 0; i < StringUtils.countMatches(criteria, "?"); i++) {
                params.add(value);
            }
        }
    }
}

