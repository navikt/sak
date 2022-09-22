package no.nav.sak.repository;


import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SakSearchCriteria {
    private List<String> aktoerId = new ArrayList<>();
    private String orgnr;
    private List<String> tema = new ArrayList<>();
    private String fagsakNr;
    private String applikasjon;

    private SakSearchCriteria() {
    }

    public static SakSearchCriteria create() {
        return new SakSearchCriteria();
    }

    public SakSearchCriteria medAktoerId(List<String> aktoerId) {
        this.aktoerId = aktoerId;
        return this;
    }

    public SakSearchCriteria medOrgnr(String orgnr) {
        this.orgnr = orgnr;
        return this;
    }

    public SakSearchCriteria medTema(List<String> tema) {
        this.tema = tema;
        return this;
    }

    public SakSearchCriteria medFagsakNr(String fagsakNr) {
        this.fagsakNr = fagsakNr;
        return this;
    }

    public SakSearchCriteria medApplikasjon(String applikasjon) {
        this.applikasjon = applikasjon;
        return this;
    }


    public List<String> getAktoerId() {
        return aktoerId;
    }

    public Optional<String> getOrgnr() {
        return Optional.ofNullable(orgnr);
    }

    List<String> getTema() {
        return tema;
    }

    Optional<String> getFagsakNr() {
        return Optional.ofNullable(fagsakNr);
    }

    public Optional<String> getApplikasjon() {
        return Optional.ofNullable(applikasjon);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
            .append("aktoerId", aktoerId)
            .append("orgnr", orgnr)
            .append("tema", tema)
            .append("fagsakNr", fagsakNr)
            .append("applikasjon", applikasjon)
            .toString();
    }
}
