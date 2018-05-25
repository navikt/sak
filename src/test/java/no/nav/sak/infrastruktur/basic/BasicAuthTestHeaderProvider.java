package no.nav.sak.infrastruktur.basic;

import no.nav.sak.SakConfiguration;

import java.io.UnsupportedEncodingException;
import java.util.Base64;

public class BasicAuthTestHeaderProvider {
    private SakConfiguration sakConfiguration = new SakConfiguration();

    public String getHeader() {
        String unencoded = sakConfiguration.getRequiredString("SYSTEMBRUKER_USERNAME") + ":" + sakConfiguration.getRequiredString("SYSTEMBRUKER_PASSWORD");
        try {
            return "Basic " + Base64.getEncoder().encodeToString(unencoded.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}
