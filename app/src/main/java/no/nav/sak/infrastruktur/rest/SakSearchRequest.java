package no.nav.sak.infrastruktur.rest;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.ws.rs.QueryParam;
import lombok.Getter;
import lombok.Setter;
import no.nav.sak.repository.SakSearchCriteria;
import no.nav.sak.validering.AtLeastOneOf;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springdoc.core.annotations.ParameterObject;

import java.util.Collections;
import java.util.List;

import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

@Setter
@ParameterObject
@AtLeastOneOf(fields = {"aktoerId", "orgnr", "fagsakNr"})
public class SakSearchRequest {
	@QueryParam("aktoerId")
	@Parameter(description = "Filtrering på saker opprettet for en aktør (person)")
	private List<String> aktoerId;
	@Getter
	@QueryParam("orgnr")
	@Parameter(description = "Filtrering på saker opprettet for en organisasjon")
	private String orgnr;
	@Getter
	@QueryParam("applikasjon")
	@Parameter(description = "Filtrering på applikasjon (iht felles kodeverk)")
	private String applikasjon;
	@Getter
	@QueryParam("tema")
	@Parameter(description = "Filtrering på tema (iht felles kodeverk)")
	private List<String> tema;
	@Getter
	@QueryParam("fagsakNr")
	@Parameter(description = "Filtrering på fagsakNr")
	private String fagsakNr;

	public SakSearchRequest() {
		//JaxRSActionDelAct
	}

	public List<String> getAktoerId() {
		if (aktoerId == null) {
			return Collections.emptyList();
		}
		return aktoerId;
	}

	SakSearchCriteria toCriteria() {
		return SakSearchCriteria.create()
				.medAktoerId(aktoerId)
				.medOrgnr(orgnr)
				.medApplikasjon(applikasjon)
				.medTema(tema)
				.medFagsakNr(fagsakNr);
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this, SHORT_PREFIX_STYLE)
				.append("aktoerId", aktoerId == null ? null : "*****")
				.append("orgnr", orgnr)
				.append("applikasjon", applikasjon)
				.append("tema", tema)
				.append("fagsaknr", fagsakNr)
				.toString();
	}
}
