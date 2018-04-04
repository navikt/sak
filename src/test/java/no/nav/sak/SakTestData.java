package no.nav.sak;

import org.apache.commons.lang3.RandomStringUtils;

import java.time.LocalDateTime;


public class SakTestData {
    private String tema = RandomStringUtils.randomAlphabetic(3);
    private String aktoerId = RandomStringUtils.randomAlphabetic(9);
    private String orgnr;
    private String fagsakNr;
    private String applikasjon = RandomStringUtils.randomAlphabetic(3);
    private String opprettetAv = RandomStringUtils.randomAlphabetic(8);
    private LocalDateTime opprettetTidspunkt = LocalDateTime.now();

    public Sak build() {
        return new Sak.Builder()
            .medAktoerId(aktoerId)
            .medOrgnr(orgnr)
            .medTema(tema)
            .medFagsakNr(fagsakNr)
            .medApplikasjon(applikasjon)
            .medOpprettetAv(opprettetAv)
            .medOpprettetTidspunkt(opprettetTidspunkt)
            .build();
    }


    public SakTestData aktoerId(String aktoerId) {
        this.aktoerId = aktoerId;
        this.orgnr = null;
        return this;
    }

    SakTestData orgnr(String orgnr) {
        this.orgnr = orgnr;
        this.aktoerId = null;
        return this;
    }

    SakTestData tema(String tema) {
        this.tema = tema;
        return this;
    }

    SakTestData fagsakNr(String fagsakNr) {
        this.fagsakNr = fagsakNr;
        return this;
    }

    SakTestData applikasjon(String applikasjon) {
        this.applikasjon = applikasjon;
        return this;
    }
}

