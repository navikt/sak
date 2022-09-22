package no.nav.sak;

import io.prometheus.client.hotspot.DefaultExports;
import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import io.swagger.v3.oas.integration.OpenApiConfigurationException;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import lombok.extern.slf4j.Slf4j;
import no.nav.resilience.ResilienceConfig;
import no.nav.sak.infrastruktur.CorrelationFilter;
import no.nav.sak.infrastruktur.DefaultExceptionMapper;
import no.nav.sak.infrastruktur.MethodNotAllowedExceptionMapper;
import no.nav.sak.infrastruktur.NotFoundExceptionMapper;
import no.nav.sak.infrastruktur.ParamExceptionMapper;
import no.nav.sak.infrastruktur.PrometheusFilter;
import no.nav.sak.infrastruktur.abac.SakPEP;
import no.nav.sak.infrastruktur.authentication.AuthenticationFilter;
import no.nav.sak.infrastruktur.rest.ExternalApiExceptionMapper;
import no.nav.sak.infrastruktur.rest.ServiceUnavailableExceptionMapper;
import no.nav.sak.repository.Database;
import no.nav.sak.repository.SakRepository;
import no.nav.sak.validering.ConstraintValidationExceptionMapper;
import no.nav.sikkerhet.abac.ABACClient;
import no.nav.sikkerhet.authentication.AuthenticationResult;
import no.nav.sikkerhet.authentication.Authenticator;
import no.nav.sikkerhet.authentication.basic.BasicAuthenticator;
import no.nav.sikkerhet.authentication.basic.LdapConfiguration;
import no.nav.sikkerhet.authentication.oidc.OidcTokenValidator;
import no.nav.sikkerhet.authentication.saml.SAMLValidator;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.jose4j.jwk.HttpsJwks;
import org.jose4j.keys.resolvers.HttpsJwksVerificationKeyResolver;
import org.jose4j.keys.resolvers.VerificationKeyResolver;
import org.opensaml.core.config.InitializationException;
import org.opensaml.core.config.InitializationService;
import org.slf4j.bridge.SLF4JBridgeHandler;

import javax.sql.DataSource;
import javax.ws.rs.ApplicationPath;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.logging.Logger.getLogger;

@ApplicationPath("/api")
@Slf4j
public class SakApplication extends ResourceConfig {

    @SuppressWarnings("WeakerAccess") //Påkrevd public
    public SakApplication(Database database) {
        DefaultExports.initialize();
        final SakConfiguration sakConfiguration = new SakConfiguration();

        migrateSak(database.getDataSource());
        registerApiResources(database, sakConfiguration);
        registerFilters(sakConfiguration);
        registerExceptionmappers();
        registerFeatures();
        registerSwaggerResources();
        initSAML();

        log.info("Jersey-Application ferdig initialisert");
    }

    void migrateSak(final DataSource dataSource) {
        // noop
    }

    void registerApiResources(final Database database, final SakConfiguration sakConfiguration) {
        ABACClient abacClient = createAbacClient(sakConfiguration);
        register(new SakResource(
            new SakRepository(database),
            new SakPEP(abacClient, ResilienceConfig.ofDefaults()))
        );
    }

    void registerAuthenticationFilter(final SakConfiguration sakConfiguration) {
        Map<String, VerificationKeyResolver> resolvers = new HashMap<>();
        List<OIDCIssuer> supportedIssuers = Arrays.asList(
            new OIDCIssuer(sakConfiguration.getRequiredString("OPENIDCONNECT_ISSO_ISSUER"), sakConfiguration.getRequiredString("OPENIDCONNECT_ISSO_JWKS")),
            new OIDCIssuer(sakConfiguration.getRequiredString("STS_ISSUER"), sakConfiguration.getRequiredString("STS_JWKS"))
        );

        for(OIDCIssuer issuer: supportedIssuers) {
            resolvers.put(issuer.issuer, new HttpsJwksVerificationKeyResolver(new HttpsJwks(issuer.jwks)));
        }


        final OidcTokenValidator oidcTokenValidator = new OidcTokenValidator(resolvers);

        final SAMLValidator samlValidator = new SAMLValidator(
            sakConfiguration.getRequiredString("javax.net.ssl.trustStore"),
            sakConfiguration.getRequiredString("javax.net.ssl.trustStorePassword"));

        final LdapConfiguration ldapConfiguration = LdapConfiguration.builder()
                .withUrl(sakConfiguration.getRequiredString("LDAP_URL"))
                .withBindUser(sakConfiguration.getRequiredString("LDAP_USERNAME"))
                .withBindPassword(sakConfiguration.getString("LDAP_PASSWORD", null))
                .withServiceUserBaseDN(sakConfiguration.getRequiredString("LDAP_SERVICEUSER_BASEDN"))
                .build();

        final CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
            .withCache("basicAuth",
                CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, AuthenticationResult.class, ResourcePoolsBuilder.heap(100))
                    .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofMinutes(5))))
            .build();
        cacheManager.init();

        final Cache<String, AuthenticationResult> cache =
            cacheManager.getCache("basicAuth", String.class, AuthenticationResult.class);

        final BasicAuthenticator basicAuthenticator = new BasicAuthenticator(ldapConfiguration, cache);
        final Authenticator authenticator = new Authenticator(oidcTokenValidator, samlValidator, basicAuthenticator);
        register(new AuthenticationFilter(authenticator,ResilienceConfig.ofDefaults()));
    }

    protected Database createDatabase(final DataSource dataSource) {
        return new Database(dataSource);
    }

    private void registerFeatures() {
        register(new JacksonFeature());
        registerLoggingFeature();
    }

    private void registerExceptionmappers() {
        register(new ConstraintValidationExceptionMapper());
        register(new DefaultExceptionMapper());
        register(new NotFoundExceptionMapper());
        register(new MethodNotAllowedExceptionMapper());
        register(new ParamExceptionMapper());
        register(new ExternalApiExceptionMapper());
        register(new ServiceUnavailableExceptionMapper());
    }

    private void registerFilters(final SakConfiguration sakConfiguration) {
        register(new CorrelationFilter());
        register(new PrometheusFilter());
        registerAuthenticationFilter(sakConfiguration);
    }

    private void initSAML() {
        try {
            InitializationService.initialize();
        } catch (InitializationException e) {
            throw new IllegalStateException("Feilet under initialisering av SAML", e);
        }
    }

    protected ABACClient createAbacClient(SakConfiguration sakConfiguration) {
            return new ABACClient(
                sakConfiguration.getRequiredString("ABAC_PDP_ENDPOINT"),
                createHttpClient(sakConfiguration));
    }

    private HttpClient createHttpClient(final SakConfiguration sakConfiguration) {

        final int timeout = Integer.parseInt(sakConfiguration.getString("ABAC_READ_TIMEOUT", "5000"));
        final RequestConfig requestConfig = RequestConfig
            .custom()
            .setConnectionRequestTimeout(timeout)
            .setSocketTimeout(timeout)
            .setConnectTimeout(timeout)
            .build();


        final HttpClientBuilder httpClientBuilder = HttpClientBuilder
            .create()
            .setDefaultRequestConfig(requestConfig);

        final UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(
            sakConfiguration.getRequiredString("SRVSAK_USERNAME"),
            sakConfiguration.getRequiredString("SRVSAK_PASSWORD"));

        final CredentialsProvider provider = new BasicCredentialsProvider();
        provider.setCredentials(AuthScope.ANY, credentials);
        httpClientBuilder.setDefaultCredentialsProvider(provider);

        return httpClientBuilder.build();
    }

    private void registerSwaggerResources() {

        OpenAPI openAPI = new OpenAPI();

        Info info = new Info()
                .title("Sak API")
                .version("v1");

        openAPI.info(info);

        final SwaggerConfiguration swaggerConfiguration = new SwaggerConfiguration()
                .openAPI(openAPI)
                .prettyPrint(true)
                .resourceClasses(Stream.of("no.nav.sak.SakResource").collect(Collectors.toSet()));

        try {
            new JaxrsOpenApiContextBuilder<>()
                    .openApiConfiguration(swaggerConfiguration)
                    .application(this)
                    .buildContext(true);
        } catch (OpenApiConfigurationException e) {
            throw new IllegalStateException(e);
        }

        register(OpenApiResource.class);
    }

    private void registerLoggingFeature() {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
        register(new LoggingFeature(getLogger(LoggingFeature.class.getName()), Level.INFO, LoggingFeature.Verbosity.PAYLOAD_TEXT, LoggingFeature.DEFAULT_MAX_ENTITY_SIZE));
    }


    static class OIDCIssuer {
        private String issuer;
        private String jwks;

        OIDCIssuer(String issuer, String jwksUri) {
            this.issuer = issuer;
            this.jwks = jwksUri;
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("issuer", issuer)
                .append("jwks", jwks)
                .toString();
        }
    }
}
