package no.nav.sak;

import io.swagger.annotations.ApiParam;
import no.nav.sak.validering.AtLeastOneOf;
import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.ws.rs.QueryParam;
import java.util.List;

import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

@AtLeastOneOf(fields = {"aktoerId", "orgnr", "fagsakNr"})
public class SakSearchRequest {
    @QueryParam("aktoerId")
    @ApiParam("Filtrering på saker opprettet for en aktør (person)")
    private List<String> aktoerId;
    @QueryParam("orgnr")
    @ApiParam("Filtrering på saker opprettet for en organisasjon")
    private String orgnr;
    @QueryParam("applikasjon")
    @ApiParam("Filtrering på applikasjon (iht felles kodeverk)")
    private String applikasjon;
    @QueryParam("tema")
    @ApiParam("Filtrering på tema (iht felles kodeverk)")
    private List<String> tema;
    @QueryParam("fagsakNr")
    @ApiParam("Filtrering på fagsakNr")
    private String fagsakNr;

    public SakSearchRequest() {
        //JaxRSActionDelAct
    }

    public List<String> getAktoerId() {
        return aktoerId;
    }

    public void setAktoerId(List<String> aktoerId) {
        this.aktoerId = aktoerId;
    }

    public String getOrgnr() {
        return orgnr;
    }

    public void setOrgnr(String orgnr) {
        this.orgnr = orgnr;
    }

    public String getApplikasjon() {
        return applikasjon;
    }

    public void setApplikasjon(String applikasjon) {
        this.applikasjon = applikasjon;
    }

    public List<String> getTema() {
        return tema;
    }

    public void setTema(List<String> tema) {
        this.tema = tema;
    }

    public String getFagsakNr() {
        return fagsakNr;
    }

    public void setFagsakNr(String fagsakNr) {
        this.fagsakNr = fagsakNr;
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
            .append("aktoerId", aktoerId)
            .append("orgnr", orgnr)
            .append("applikasjon", applikasjon)
            .append("tema", tema)
            .append("fagsaknr", fagsakNr)
            .toString();
    }
}
