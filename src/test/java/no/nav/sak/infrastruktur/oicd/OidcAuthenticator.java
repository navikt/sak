package no.nav.sak.infrastruktur.oicd;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.cookie.BasicClientCookie;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class OidcAuthenticator {
    private final String redirectUriEncoded;
    private final URI authorizationEndpointURI;

    OidcAuthenticator(URI authorizationEndpointURI, String redirectUrl) throws Exception {
        this.authorizationEndpointURI = authorizationEndpointURI;
        this.redirectUriEncoded = URLEncoder.encode(redirectUrl, StandardCharsets.UTF_8.name());
    }

    private static Map<String, String> parseJSON(String response) {
        ObjectMapper mapper = new ObjectMapper();
        TypeFactory factory = TypeFactory.defaultInstance();
        MapType type = factory.constructMapType(HashMap.class, String.class, String.class);
        try {
            return mapper.readValue(response, type);
        } catch (IOException e) {
            throw new IllegalStateException("Kunne ikke parse json", e);
        }
    }

    String getAuthorizationCode(String brukernavn, String passord) throws IOException {
        if (brukernavn == null || passord == null || brukernavn.isEmpty() || passord.isEmpty()) {
            throw new IllegalArgumentException("Brukernavn og/eller passord mangler.");
        }
        CookieStore cookieStore = new BasicCookieStore();
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().disableRedirectHandling().setDefaultCookieStore(cookieStore).build()) {
            authenticateUser(httpClient, cookieStore, brukernavn, passord);
            return hentAuthorizationCode(httpClient);
        }
    }

    private void authenticateUser(CloseableHttpClient httpClient, CookieStore cookieStore, String brukernavn, String passord) throws IOException {
        String jsonAuthUrl = "https://isso-t.adeo.no/isso/json/authenticate";

        String template = post(httpClient, jsonAuthUrl, null, Function.identity(),
            "Authorization: Negotiate");

        ObjectMapper mapper = new ObjectMapper();
        EndUserAuthorizationTemplate json = mapper.readValue(template, EndUserAuthorizationTemplate.class);
        json.setBrukernavn(brukernavn);
        json.setPassord(passord);
        String utfyltTemplate = mapper.writeValueAsString(json);

        Function<String, String> hentSessionTokenFraResult = result -> {
            Map<String, String> stringStringMap = parseJSON(result);
            return stringStringMap.get("tokenId");
        };

        String issoCookieValue = post(httpClient, jsonAuthUrl, utfyltTemplate, hentSessionTokenFraResult);
        BasicClientCookie cookie = new BasicClientCookie("nav-isso", issoCookieValue);
        URI uri = URI.create("https://isso-t.adeo.no/isso/oauth2");
        cookie.setDomain("." + uri.getHost());
        cookie.setPath("/");
        cookie.setSecure(true);
        cookieStore.addCookie(cookie);
    }

    private <T> T post(CloseableHttpClient httpClient, String url, String data, Function<String, T> resultTransformer, String... headers) throws IOException {
        HttpPost post = new HttpPost(url);
        post.setHeader("Content-type", "application/json");
        for (String header : headers) {
            String[] h = header.split(":");
            post.setHeader(h[0], h[1]);
        }
        if (data != null) {
            post.setEntity(new StringEntity(data, "UTF-8"));
        }

        try (CloseableHttpResponse response = httpClient.execute(post)) {
            try (InputStreamReader isr = new InputStreamReader(response.getEntity().getContent(), StandardCharsets.UTF_8)) {
                try (BufferedReader br = new BufferedReader(isr)) {
                    String responseString = br.lines().collect(Collectors.joining("\n"));
                    if (response.getStatusLine().getStatusCode() == 200) {
                        return resultTransformer.apply(responseString);
                    } else {
                        throw new IllegalStateException("Uventet response fra openam");
                    }
                }
            }
        } finally {
            post.reset();
        }
    }

    private String hentAuthorizationCode(CloseableHttpClient httpClient) throws IOException {
        String url = authorizationEndpointURI + "?response_type=code&scope=openid&client_id=sak-t0&redirect_uri=" + redirectUriEncoded;
        HttpGet get = new HttpGet(url);
        get.setHeader("Content-type", "application/json");

        try (CloseableHttpResponse response = httpClient.execute(get)) {
            if (response.containsHeader("Location")) {
                Pattern pattern = Pattern.compile("code=([^&]*)");
                String locationHeader = response.getFirstHeader("Location").getValue();
                Matcher matcher = pattern.matcher(locationHeader);
                if (matcher.find()) {
                    return matcher.group(1);
                }
            }
            throw new IllegalStateException("Fant ikke autorisasjonskode");
        } finally {
            get.reset();
        }
    }
}
