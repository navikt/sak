package no.nav.sak.repository;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;
import no.nav.sak.validering.OrganisasjonsnummerValidator;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.time.LocalDateTime;
import java.util.Objects;

import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Sak {

    Long id;
    String fagsakNr;
    String tema;
    String aktoerId;
    String applikasjon;
    String orgnr;
    String opprettetAv;
    LocalDateTime opprettetTidspunkt;
    String status;

    private Sak(Builder builder) {
         this(builder.id,
         builder.fagsakNr,
         builder.tema,
         builder.aktoerId,
         builder.applikasjon,
         builder.orgnr,
         builder.opprettetAv,
         builder.opprettetTidspunkt,
         builder.status);
    }

	Sak withId(Long id) {
        return new Sak(id,
                this.fagsakNr,
                this.tema,
                this.aktoerId,
                this.applikasjon,
                this.orgnr,
                this.opprettetAv,
                this.opprettetTidspunkt,
                this.status);
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
        private String status;

        public Builder() {
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

        public Builder medSakStatus(String status) {
            this.status = status;
            return this;
        }

        public Sak build() {
            Validate.validState(!StringUtils.isNoneBlank(aktoerId, orgnr), "Kun en av aktørid eller orgnr kan være angitt");
            Validate.validState(!StringUtils.isAllBlank(aktoerId, orgnr), "Aktørid eller orgnr må være angitt");
            Validate.notBlank(tema, "Tema må være angitt");
            Validate.notBlank(opprettetAv, "OpprettetAv må være angitt");
            Validate.notNull(opprettetTidspunkt, "Opprettet tidspunkt må være angitt");
            Validate.validState(OrganisasjonsnummerValidator.isValid(orgnr));
            return new Sak(this);
        }
    }
}
