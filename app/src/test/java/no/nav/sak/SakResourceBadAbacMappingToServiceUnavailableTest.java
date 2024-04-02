package no.nav.sak;

import no.nav.sak.repository.Sak;
import no.nav.sak.repository.SakTestData;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@Disabled
public class SakResourceBadAbacMappingToServiceUnavailableTest extends AbstractSakResourceTest {

	@Test
	void hent_sak_giving_bad_abac_result_mapping_to_service_unavailable() {

		final Sak opprettetSak =
				testUtilityRepository
						.lagre(
								new SakTestData()
										.aktoerId("123")
										.build()
						);

		final ResponseEntity<?> response = executeGetRequestWithSaml(URI.create(String.valueOf(opprettetSak.getId())));

		assertThat(response.getStatusCode()).isEqualTo(SERVICE_UNAVAILABLE);
	}

	@Test
	void opprett_sak_giving_bad_abac_result_mapping_to_service_unavailable() {

		final Sak sak =
				new SakTestData()
						.aktoerId("1")
						.applikasjon("FS22")
						.build();

		createSakAndTestReponse(sak);
	}

	@Test
	void finn_saker_giving_bad_abac_result_mapping_to_service_unavailable() {

		opprett100Tilfeldigesaker();
		final String tema = RandomStringUtils.randomAlphabetic(4);
		final Sak sak = testUtilityRepository.lagre(new SakTestData().tema(tema).build());

		final ResponseEntity<?> response = executeGetRequestWithSaml(UriComponentsBuilder.newInstance()
				.queryParam("tema", sak.getTema())
				.queryParam("aktoerId", sak.getAktoerId()).build().toUri());

		assertThat(response.getStatusCode()).isEqualTo(SERVICE_UNAVAILABLE);
	}

	@Override
	protected ResponseEntity<Object> createSakAndTestReponse(final Sak sak) {

		final ResponseEntity<Object> createdResponse =
				executePostWithSaml(
						new SakJsonTestData(sak).buildJsonString()
				);

		assertThat(createdResponse.getStatusCode()).isEqualTo(SERVICE_UNAVAILABLE);

		return createdResponse;
	}
}
