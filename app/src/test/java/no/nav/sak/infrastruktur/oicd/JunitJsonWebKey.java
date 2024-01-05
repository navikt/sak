package no.nav.sak.infrastruktur.oicd;

import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jwk.RsaJwkGenerator;

public class JunitJsonWebKey {
    private static RsaJsonWebKey rsaJsonWebKey;

    private JunitJsonWebKey() {
        //Testutil
    }

    public static RsaJsonWebKey get() {
        if (rsaJsonWebKey == null) {
            try {
                rsaJsonWebKey = RsaJwkGenerator.generateJwk(2048);
                rsaJsonWebKey.setKeyId("k1");
                rsaJsonWebKey.setAlgorithm("RS256");
                rsaJsonWebKey.setUse("sig");
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
        return rsaJsonWebKey;
    }

}
