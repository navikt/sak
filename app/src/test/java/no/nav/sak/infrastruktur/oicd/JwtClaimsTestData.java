package no.nav.sak.infrastruktur.oicd;

import org.jose4j.jwt.JwtClaims;

public class JwtClaimsTestData {
    public static final String ISSUER = "issuer";

    public JwtClaims build() {
        JwtClaims claims = new JwtClaims();
        claims.setIssuer(ISSUER);
        claims.setAudience("no/nav/sak");
        claims.setExpirationTimeMinutesInTheFuture(10);
        claims.setGeneratedJwtId();
        claims.setIssuedAtToNow();
        claims.setSubject("subject");
        claims.setClaim("azp", "sak-junit");
        return claims;
    }

}
