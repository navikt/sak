package no.nav.sak.infrastruktur.basic;

import no.nav.sak.SakConfiguration;
import no.nav.sak.configuration.StsProperties;

import java.io.UnsupportedEncodingException;
import java.util.Base64;

public class BasicAuthTestHeaderProvider {

	private StsProperties stsProperties;

    public String getHeader() {
        String unencoded = stsProperties.user() + ":" + stsProperties.password();
        try {
            return "Basic " + Base64.getEncoder().encodeToString(unencoded.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}
