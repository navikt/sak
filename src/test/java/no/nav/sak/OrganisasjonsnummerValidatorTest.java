package no.nav.sak;

import no.nav.sak.validering.OrganisasjonsnummerValidator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrganisasjonsnummerValidatorTest {
    private static final String GYLDIG_ORGNR_STARTSIFFER_9 = "979312059";

    @Test
    void orgnr_skal_vaere_ugyldig_naar_det_har_mindre_enn_9_siffer() {
        assertThat(OrganisasjonsnummerValidator.isValid(GYLDIG_ORGNR_STARTSIFFER_9)).isTrue();
        assertThat(OrganisasjonsnummerValidator.isValid(GYLDIG_ORGNR_STARTSIFFER_9.substring(0, 8))).isFalse();
    }

    @Test
    void orgnr_skal_vaere_ugyldig_naar_det_har_mer_enn_9_siffer() {
        assertThat(OrganisasjonsnummerValidator.isValid("979312059")).isTrue();
        assertThat(OrganisasjonsnummerValidator.isValid("9793120597")).isFalse();
    }

    @Test
    void orgnrSkalIkkeInneholdeBokstaver() {
        assertThat(OrganisasjonsnummerValidator.isValid("979312059")).isTrue();
        assertThat(OrganisasjonsnummerValidator.isValid("97931k059")).isFalse();
    }

    @Test
    void orgnrSkalIkkeInneholdeSpesialtegn() {
        assertThat(OrganisasjonsnummerValidator.isValid("979312059")).isTrue();
        assertThat(OrganisasjonsnummerValidator.isValid("97931*059")).isFalse();
    }

    @Test
    void orgnrSkalGodtaSyntetiskOrgnr() {
        assertThat(OrganisasjonsnummerValidator.isValid("279312059")).isTrue();
    }

    @Test
    void orgnrSkalIkkeInneholdeEksponensielltegn() {
        assertThat(OrganisasjonsnummerValidator.isValid("979312059")).isTrue();
        assertThat(OrganisasjonsnummerValidator.isValid("9793120e5")).isFalse();
    }


}
