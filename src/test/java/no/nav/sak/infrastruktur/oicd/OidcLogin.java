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
import no.nav.sak.SakConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

public class OidcLogin {
    private SakConfiguration sakConfiguration = new SakConfiguration();

    public String getIdToken() throws Exception {
        OIDCProviderMetadata providerMetadata = getOidcProviderMetadata();
        String redirectUrl = "https://sak.nais.preprod.local/";

        String testLoginUsername = sakConfiguration.getRequiredString("testLoginUsername");
        String testLoginPassword = sakConfiguration.getRequiredString("testLoginPassword");

        OidcAuthenticator oidcAuthenticator = new OidcAuthenticator(providerMetadata.getAuthorizationEndpointURI(), redirectUrl);
        String authorizationCode = oidcAuthenticator.getAuthorizationCode(testLoginUsername, testLoginPassword);

        String clientId = sakConfiguration.getRequiredString("isso-rp-issuer");
        String clientSecret = sakConfiguration.getRequiredString("OpenIdConnectAgent.password");

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
