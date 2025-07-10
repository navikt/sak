package no.nav.sak.repository;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;

import java.time.LocalDateTime;

public class SakTestData {
	private static final String[] gyldigeOrgnr = {"999263550", "991012133", "895106402"};
	// Se V6__populate_fagomrade.sql
	// MOB er inaktiv
	private static final String[] populatedAktivTema = {"AAP", "FOR", "PEN"};
	private String tema = choosePopulatedTema();
	private String aktoerId = RandomStringUtils.secure().nextNumeric(13);
	private String orgnr;
	private String fagsakNr;
	private String applikasjon;
	private final String opprettetAv = RandomStringUtils.secure().nextAlphabetic(8);
	private final LocalDateTime opprettetTidspunkt = LocalDateTime.now();

	public Sak build() {
		return Sak.builder()
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

	public SakTestData orgnr(String orgnr) {
		this.orgnr = orgnr;
		this.aktoerId = null;
		return this;
	}

	public SakTestData tema(String tema) {
		this.tema = tema;
		return this;
	}

	public SakTestData fagsakNr(String fagsakNr) {
		this.fagsakNr = fagsakNr;
		return this;
	}

	public SakTestData applikasjon(String applikasjon) {
		this.applikasjon = applikasjon;
		return this;
	}

	public static String generateValidOrgnr() {
		return gyldigeOrgnr[RandomUtils.secure().randomInt(0, gyldigeOrgnr.length)];
	}

	public static String choosePopulatedTema() {
		return populatedAktivTema[RandomUtils.secure().randomInt(0, populatedAktivTema.length)];
	}
}

