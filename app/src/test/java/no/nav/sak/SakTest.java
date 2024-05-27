package no.nav.sak;

import no.nav.sak.repository.Sak;
import no.nav.sak.repository.SakTestData;
import org.junit.jupiter.api.Test;

import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static org.assertj.core.api.Assertions.assertThat;

public class SakTest {

	@Test
	public void verifyDuplicateMethodsAreConsistent() {
		SakTestData sakTestData = new SakTestData().fagsakNr(randomNumeric(6));
		Sak sak = sakTestData.medId(1L).build();
		Sak sak2 = new SakTestData().medId(2L).fagsakNr(randomNumeric(6)).build();
		Sak sak3 = new SakTestData().medId(3L).duplicateOf(sakTestData).build();

		assertThat(sak).isNotEqualTo(sak2);
		assertThat(sak).isNotEqualTo(sak3);
		assertThat(sak2).isNotEqualTo(sak3);
		assertThat(sak.hashCode()).isNotEqualTo(sak2.hashCode());
		assertThat(sak.hashCode()).isNotEqualTo(sak3.hashCode());
		assertThat(sak2.hashCode()).isNotEqualTo(sak3.hashCode());

		assertThat(sak.hashForDuplikat()).isNotEqualTo(sak2.hashForDuplikat());
		assertThat(sak.erDuplikat(sak2)).isFalse();

		assertThat(sak.hashForDuplikat()).isEqualTo(sak3.hashForDuplikat());
		assertThat(sak.erDuplikat(sak3)).isTrue();

		assertThat(sak2.hashForDuplikat()).isNotEqualTo(sak3.hashForDuplikat());
		assertThat(sak2.erDuplikat(sak3)).isFalse();
	}

}
