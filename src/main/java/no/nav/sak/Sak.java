package no.nav.sak;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.time.LocalDateTime;
import java.util.Objects;

import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

public class Sak {

    private final String tema;
    private final String applikasjon;
    private final String fagsakNr;
    private final String aktoerId;
    private final String orgnr;
    private final String opprettetAv;
    private final LocalDateTime opprettetTidspunkt;
    private Long id;

    private Sak(Builder builder) {
        this.id = builder.id;
        this.fagsakNr = builder.fagsakNr;
        this.tema = builder.tema;
        this.aktoerId = builder.aktoerId;
        this.applikasjon = builder.applikasjon;
        this.orgnr = builder.orgnr;
        this.opprettetAv = builder.opprettetAv;
        this.opprettetTidspunkt = builder.opprettetTidspunkt;
    }

    Long getId() {
        return id;
    }

    void setId(Long id) {
        this.id = id;
    }

    String getTema() {
        return tema;
    }

    String getApplikasjon() {
        return applikasjon;
    }

    String getFagsakNr() {
        return fagsakNr;
    }

    String getAktoerId() {
        return aktoerId;
    }

    String getOrgnr() {
        return orgnr;
    }

    String getOpprettetAv() {
        return opprettetAv;
    }

    LocalDateTime getOpprettetTidspunkt() {
        return opprettetTidspunkt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Sak sak = (Sak) o;
        return Objects.equals(id, sak.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, SHORT_PREFIX_STYLE)
            .append("id", id)
            .toString();
    }


    public static final class Builder {
        private Long id;
        private String tema;
        private String applikasjon;
        private String fagsakNr;
        private String aktoerId;
        private String orgnr;
        private String opprettetAv;
        private LocalDateTime opprettetTidspunkt;

        Builder() {
        }

        public static Builder enSak() {
            return new Builder();
        }

        Builder medId(Long id) {
            this.id = id;
            return this;
        }

        public Builder medTema(String tema) {
            this.tema = tema;
            return this;
        }

        public Builder medApplikasjon(String applikasjon) {
            this.applikasjon = applikasjon;
            return this;
        }

        public Builder medFagsakNr(String fagsakNr) {
            this.fagsakNr = fagsakNr;
            return this;
        }

        public Builder medAktoerId(String aktoerId) {
            this.aktoerId = aktoerId;
            return this;
        }

        public Builder medOrgnr(String orgnr) {
            this.orgnr = orgnr;
            return this;
        }

        public Builder medOpprettetAv(String opprettetAv) {
            this.opprettetAv = opprettetAv;
            return this;
        }

        public Builder medOpprettetTidspunkt(LocalDateTime opprettetTidspunkt) {
            this.opprettetTidspunkt = opprettetTidspunkt;
            return this;
        }

        public Sak build() {
            Validate.validState(!StringUtils.isNoneBlank(aktoerId, orgnr), "Kun en av aktørid eller orgnr kan være angitt");
            Validate.validState(!StringUtils.isAllBlank(aktoerId, orgnr), "Aktørid eller orgnr må være angitt");
            Validate.notBlank(tema, "Tema må være angitt");
            Validate.notBlank(applikasjon, "Applikasjon må være angitt");
            Validate.notBlank(opprettetAv, "OpprettetAv må være angitt");
            Validate.notNull(opprettetTidspunkt, "Opprettet tidspunkt må være angitt");
            return new Sak(this);
        }
    }
}
