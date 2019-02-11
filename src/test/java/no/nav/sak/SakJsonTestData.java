package no.nav.sak;

import com.google.gson.JsonObject;

public class SakJsonTestData {
    private Sak sak;
    private String applikasjon;
    private String tema;

    public SakJsonTestData(Sak sak) {
        this.sak = sak;
    }

    SakJsonTestData() {
    }

    public SakJsonTestData medApplikasjon(String applikasjon) {
        this.applikasjon = applikasjon;
        return this;
    }

    public SakJsonTestData medTema(String tema) {
        this.tema = tema;
        return this;
    }

    public String buildJsonString() {
        JsonObject jsonObject = new JsonObject();
        if (sak != null) {
            jsonObject.addProperty("tema", sak.getTema());
            jsonObject.addProperty("aktoerId", sak.getAktoerId());
            jsonObject.addProperty("orgnr", sak.getOrgnr());
            jsonObject.addProperty("applikasjon", sak.getApplikasjon());
            jsonObject.addProperty("fagsakNr", sak.getFagsakNr());
        } else {
            jsonObject.addProperty("tema", tema);
            jsonObject.addProperty("applikasjon", applikasjon);
        }
        return jsonObject.toString();
    }

}
