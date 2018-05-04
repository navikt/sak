package no.nav.sak;

import no.nav.sak.validering.OrganisasjonsnummerValidator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrganisasjonsnummerValidatorTest {
    private static final int[] ORGNR_VEKTER = {3, 2, 7, 6, 5, 4, 3, 2};
    private static final String GYLDIG_ORGNR_STARTSIFFER_9 = "979312059";
    private static final String GYLDIG_ORGNR_STARTSIFFER_8 = "891046642";

    @Test
    void orgnr_skal_vaere_ugyldig_naar_det_har_mindre_enn_9_siffer() {
        assertThat(OrganisasjonsnummerValidator.isValid(GYLDIG_ORGNR_STARTSIFFER_9)).isTrue();
        assertThat(OrganisasjonsnummerValidator.isValid(GYLDIG_ORGNR_STARTSIFFER_9.substring(0, 8) + "5")).isFalse();
    }

    @Test
    void orgnr_skal_vaere_ugyldig_naar_det_har_mer_enn_9_siffer() {
        assertThat(OrganisasjonsnummerValidator.isValid("979312059")).isTrue();
        assertThat(OrganisasjonsnummerValidator.isValid(GYLDIG_ORGNR_STARTSIFFER_9.substring(0, 9) + "7")).isFalse();
    }

    @Test
    void orgnr_skal_bare_vaere_ugyldig_naar_det_ikke_starter_paa_8_eller_9() {
        assertOrgnrMaaStartePaa8eller9(GYLDIG_ORGNR_STARTSIFFER_9);
        assertOrgnrMaaStartePaa8eller9(GYLDIG_ORGNR_STARTSIFFER_8);
    }

    void assertOrgnrMaaStartePaa8eller9(String gyldigOrgnr) {
        String gyldigStartSiffer = gyldigOrgnr.substring(0, 1);
        String siste8SifferAvGyldigOrgnr = gyldigOrgnr.substring(1, 9);

        assertThat(OrganisasjonsnummerValidator.isValid("1" + siste8SifferAvGyldigOrgnr)).isFalse();
        assertThat(OrganisasjonsnummerValidator.isValid("2" + siste8SifferAvGyldigOrgnr)).isFalse();
        assertThat(OrganisasjonsnummerValidator.isValid("3" + siste8SifferAvGyldigOrgnr)).isFalse();
        assertThat(OrganisasjonsnummerValidator.isValid("4" + siste8SifferAvGyldigOrgnr)).isFalse();
        assertThat(OrganisasjonsnummerValidator.isValid("5" + siste8SifferAvGyldigOrgnr)).isFalse();
        assertThat(OrganisasjonsnummerValidator.isValid("6" + siste8SifferAvGyldigOrgnr)).isFalse();
        assertThat(OrganisasjonsnummerValidator.isValid("7" + siste8SifferAvGyldigOrgnr)).isFalse();
        assertThat(OrganisasjonsnummerValidator.isValid(gyldigStartSiffer + siste8SifferAvGyldigOrgnr)).isTrue();
    }

    @Test
    void orgnr_skal_bare_vaere_ugyldig_naar_checksum_ikke_er_riktig() {
        assertThat(OrganisasjonsnummerValidator.isValid(GYLDIG_ORGNR_STARTSIFFER_9)).isTrue();
        assertThat(OrganisasjonsnummerValidator.isValid(GYLDIG_ORGNR_STARTSIFFER_9.substring(0, 8) + "5")).isFalse();

        assertThat(OrganisasjonsnummerValidator.isValid(GYLDIG_ORGNR_STARTSIFFER_8)).isTrue();
        assertThat(OrganisasjonsnummerValidator.isValid(GYLDIG_ORGNR_STARTSIFFER_8.substring(0, 8) + "5")).isFalse();
    }

    @Test
    void orgnr_skal_vaere_ugyldig_naar_checksum_er_10() {
        String orgnrMedRest1 = "822112345";
        int orgnrSum = sum(orgnrMedRest1.substring(0, 8));
        assertThat(orgnrSum % 11).isEqualTo(1);
        assertThat(OrganisasjonsnummerValidator.isValid(orgnrMedRest1)).isFalse();
    }

    int sum(String orgnrUtenChecksum) {
        int sum = 0;
        for (int i = 0; i < 8; i++) {
            int verdi = Integer.parseInt(orgnrUtenChecksum.substring(i, i + 1));
            sum += (ORGNR_VEKTER[i] * verdi);
        }
        return sum;
    }
}
