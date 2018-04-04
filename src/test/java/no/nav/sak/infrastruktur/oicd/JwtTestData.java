package no.nav.sak.infrastruktur.oicd;

import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.lang.JoseException;

public class JwtTestData {
    private JwtClaims jwtClaims = new JwtClaimsTestData().build();

    public String build() {
        RsaJsonWebKey rsaJsonWebKey = JunitJsonWebKey.get();
        JsonWebSignature jws = new JsonWebSignature();
        jws.setPayload(jwtClaims.toJson());
        jws.setKey(rsaJsonWebKey.getPrivateKey());
        jws.setKeyIdHeaderValue(rsaJsonWebKey.getKeyId());
        jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);

        String jwt;
        try {
            jwt = jws.getCompactSerialization();
        } catch (JoseException e) {
            throw new IllegalStateException(e);
        }
        return jwt;
    }

    public JwtTestData claims(JwtClaims jwtClaims) {
        this.jwtClaims = jwtClaims;
        return this;
    }
}
