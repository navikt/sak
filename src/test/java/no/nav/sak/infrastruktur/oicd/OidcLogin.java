package no.nav.sak.infrastruktur.oicd;

import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponseParser;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import org.apache.commons.lang3.Validate;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

import static java.lang.System.getenv;

public class OidcLogin {
    public String getIdToken() throws Exception {
        OIDCProviderMetadata providerMetadata = getOidcProviderMetadata();
        String redirectUrl = "https://sak.nais.preprod.local/";

        String testLoginUsername = Validate.notNull(getenv("testLoginUsername"), "Env property 'testLoginUsername' er påkrevd");
        String testLoginPassword = Validate.notNull(getenv("testLoginPassword"), "Env property 'testLoginPassword' er påkrevd");

        OidcAuthenticator oidcAuthenticator = new OidcAuthenticator(providerMetadata.getAuthorizationEndpointURI(), redirectUrl);
        String authorizationCode = oidcAuthenticator.getAuthorizationCode(testLoginUsername, testLoginPassword);

        String clientId = Validate.notNull(getenv("isso-rp-issuer"), "Env property 'isso-rp-issuer' er påkrevd");
        String clientSecret = Validate.notNull(getenv("OpenIdConnectAgent.password"), "Env property 'OpenIdConnectAgent.password' er påkrevd");

        TokenRequest tokenRequest = new TokenRequest(
            providerMetadata.getTokenEndpointURI(),
            new ClientSecretBasic(new ClientID(clientId), new Secret(clientSecret)),
            new AuthorizationCodeGrant(new AuthorizationCode(authorizationCode), new URI(redirectUrl))
        );
        HTTPResponse httpTokenResponse = tokenRequest.toHTTPRequest().send();
        OIDCTokenResponse tokenResponse = (OIDCTokenResponse) OIDCTokenResponseParser.parse(httpTokenResponse);
        return tokenResponse.getOIDCTokens().getIDTokenString();
    }

    private OIDCProviderMetadata getOidcProviderMetadata() throws IOException, ParseException {
        URL providerConfigurationURI = new URL("https://isso-t.adeo.no/isso/oauth2/.well-known/openid-configuration");
        String providerInfo;
        InputStream stream = providerConfigurationURI.openStream();
        try (java.util.Scanner s = new java.util.Scanner(stream)) {
            providerInfo = s.useDelimiter("\\A").hasNext() ? s.next() : "";
        }
        return OIDCProviderMetadata.parse(providerInfo);
    }

}
