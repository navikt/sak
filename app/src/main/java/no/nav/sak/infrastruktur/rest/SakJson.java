package no.nav.sak.infrastruktur.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import no.nav.sak.repository.Sak;
import no.nav.sak.validering.NotNullWhenDependsOnHasValue;
import no.nav.sak.validering.ExactlyOneOf;
import no.nav.sak.validering.Organisasjonsnummer;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@ExactlyOneOf(fields = {"aktoerId", "orgnr"})
@NotNullWhenDependsOnHasValue(field = "applikasjon", dependsOnField = "fagsakNr")
public class SakJson {
    private Long id;

    @NotNull(message = "{no.nav.sak.tema.NotNull}")
    @Size(max = 40)
    private String tema;

    @Size(max = 40)
    private String applikasjon;

    @Size(message = "{no.nav.sak.aktoerId.Size}", max = 40)
    private String aktoerId;

    @Organisasjonsnummer
    @Size(message = "{no.nav.sak.orgnr.Size}", max = 9)
    private String orgnr;

    private String fagsakNr;

    private String opprettetAv;
    private LocalDateTime opprettetTidspunkt;


    public SakJson() {
        //JaxRS
    }

    SakJson(Sak sak) {
        this.id = sak.getId();
        this.tema = sak.getTema();
        this.aktoerId = sak.getAktoerId();
        this.orgnr = sak.getOrgnr();
        this.fagsakNr = sak.getFagsakNr();
        this.applikasjon = sak.getApplikasjon();
        this.opprettetAv = sak.getOpprettetAv();
        this.opprettetTidspunkt = sak.getOpprettetTidspunkt();
    }


    @JsonProperty("id")
    public Long getId() {
        return id;
    }

    @JsonProperty("tema")
    @Schema(description = "Kode for tema iht. felles kodeverk", example = "AAP")
    public String getTema() {
        return tema;
    }

    public void setTema(String tema) {
        this.tema = tema;
    }

    @JsonProperty("applikasjon")
    @Schema(description = "Kode for applikasjon iht. felles kodeverk. For generelle saker skal denne være blank (Legacy = FS22). For fagsaker, i.e saker der det refereres" +
        "til et fagsaknr, så skal man benytte applikasjonskoden for fagsystemet der saken behandles", example = "IT01")
    public String getApplikasjon() {
        return applikasjon;
    }

    public void setApplikasjon(String applikasjon) {
        this.applikasjon = applikasjon;
    }

    @JsonProperty("aktoerId")
    @Schema(description = "Id til aktøren saken gjelder", example = "10038999999")
    public String getAktoerId() {
        return aktoerId;
    }

    public void setAktoerId(String aktoerId) {
        this.aktoerId = aktoerId;
    }

    @JsonProperty("opprettetAv")
    @Schema(description = "Brukerident til den som opprettet saken")
    public String getOpprettetAv() {
        return opprettetAv;
    }

    @JsonProperty("orgnr")
    @Schema(description = "Orgnr til foretaket saken gjelder")
    public String getOrgnr() {
        return orgnr;
    }

    public void setOrgnr(String orgnr) {
        this.orgnr = orgnr;
    }

    @JsonProperty("fagsakNr")
    @Schema(description = "Fagsaknr for den aktuelle saken - hvis aktuelt")
    public String getFagsakNr() {
        return fagsakNr;
    }

    public void setFagsakNr(String fagsakNr) {
        this.fagsakNr = fagsakNr;
    }

    @JsonProperty("opprettetTidspunkt")
    @Schema(description = "Opprettet tidspunkt iht. ISO-8601")
    public String getOpprettetTidspunkt() {
        // Trunkert til millis presisjon pga BRUT001 og gsak SakV1 SOAP API.
        return ZonedDateTime.of(opprettetTidspunkt, ZoneId.systemDefault()).truncatedTo(ChronoUnit.MILLIS).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    Sak toSak(String opprettetAv) {
        return new Sak.Builder()
            .medAktoerId(aktoerId)
            .medOrgnr(orgnr)
            .medTema(tema)
            .medFagsakNr(fagsakNr)
            .medApplikasjon(applikasjon)
            .medOpprettetAv(opprettetAv)
            .medOpprettetTidspunkt(LocalDateTime.now())
            .build();
    }
}
