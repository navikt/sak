package no.nav.sak.infrastruktur.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import no.nav.sak.repository.Sak;
import no.nav.sak.validering.ExactlyOneOf;
import no.nav.sak.validering.NotNullWhenDependsOnHasValue;
import no.nav.sak.validering.Organisasjonsnummer;

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

	@Size(message = "{no.nav.sak.aktoerId.Size}", max = 13, min = 13)
	private String aktoerId;

	@Organisasjonsnummer
	@Size(message = "{no.nav.sak.orgnr.Size}", max = 9, min = 9)
	private String orgnr;

	private String fagsakNr;

	private String sakStatus;
	private String opprettetAv;
	private LocalDateTime opprettetTidspunkt;


	public SakJson() {
		//JaxRS
	}

	SakJson(Sak sak) {
		this(sak, sak.getSakId());
	}

	SakJson(Sak sak, long id) {
		this.id = id;
		this.tema = sak.getTema();
		this.aktoerId = sak.getAktoerId();
		this.orgnr = sak.getOrgnr();
		this.fagsakNr = sak.getFagsakNr();
		this.applikasjon = sak.getApplikasjon();
		this.sakStatus = sak.getSakStatus();
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
	@Schema(description = """
			Kode for applikasjon.
			For generelle saker skal denne være 'FS22'.
			Finner du ikke ditt fagsystem i dokumentasjon på <a href="https://confluence.adeo.no/spaces/BOA/pages/313346837/opprettJournalpost">opprettJournalpost</a> under fagsaksystem? Ta kontakt med Team dokumentløsninger.
			""", example = "IT01")
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

	@JsonProperty("sakStatus")
	@Schema(description = "Status for den aktuelle saken - kan være null")
	public String getSakStatus() {
		return sakStatus;
	}

	@JsonProperty("opprettetTidspunkt")
	@Schema(description = "Opprettet tidspunkt iht. ISO-8601")
	public String getOpprettetTidspunkt() {
		// Trunkert til millis presisjon pga BRUT001 og gsak SakV1 SOAP API.
		return ZonedDateTime.of(opprettetTidspunkt, ZoneId.systemDefault()).truncatedTo(ChronoUnit.MILLIS).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
	}

	Sak toSak(String opprettetAv) {
		return Sak.builder()
				.medAktoerId(aktoerId)
				.medOrgnr(orgnr)
				.medTema(tema)
				.medFagsakNr(fagsakNr)
				.medApplikasjon(applikasjon)
				.medSakStatus(sakStatus)
				.medOpprettetAv(opprettetAv)
				.medOpprettetTidspunkt(LocalDateTime.now())
				.build();
	}
}
