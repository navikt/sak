package no.nav.sak;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.configuration2.CompositeConfiguration;
import org.apache.commons.configuration2.EnvironmentConfiguration;
import org.apache.commons.configuration2.SystemConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.Validate;

import java.io.File;

@Slf4j
public class SakConfiguration {
    private final CompositeConfiguration compositeConfiguration = new CompositeConfiguration();

    public SakConfiguration() {
        compositeConfiguration.addConfiguration(new SystemConfiguration());
        compositeConfiguration.addConfiguration(new EnvironmentConfiguration());
        log.info("Konfigurasjon lastet fra system- og miljøvariabler");
        addProperties("sak.properties");
        addProperties("/var/run/secrets/nais.io/vault/secrets.properties");
    }

    private void addProperties(String path) {
        try {
            compositeConfiguration.addConfiguration(new Configurations().properties(new File(path)));
            log.info("Konfigurasjon lastet fra {}", path);
        } catch (ConfigurationException e) {
            log.info("Fant ikke {}", path);
        }
    }


    boolean getBoolean(String key, boolean defaultValue) {
        return compositeConfiguration.getBoolean(key, defaultValue);
    }

    public String getRequiredString(String key) {
        checkRequired(key);
        return compositeConfiguration.getString(key);
    }

    String getString(String key, String defaultValue) {
        return compositeConfiguration.getString(key, defaultValue);
    }

    private void checkRequired(String key) {
        Validate.validState(compositeConfiguration.containsKey(key), "Fant ikke konfigurasjonsnøkkel %s", key);
    }
}
