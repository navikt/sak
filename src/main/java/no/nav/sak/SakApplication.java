package no.nav.sak;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.metrics.prometheus.PrometheusMetricsTrackerFactory;
import io.prometheus.client.hotspot.DefaultExports;
import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.listing.ApiListingResource;
import io.swagger.jaxrs.listing.SwaggerSerializers;
import no.nav.sak.infrastruktur.*;
import no.nav.sak.infrastruktur.abac.SakPEP;
import no.nav.sak.infrastruktur.authentication.AuthenticationFilter;
import no.nav.sak.validering.ConstraintValidationExceptionMapper;
import no.nav.sikkerhet.abac.ABACClient;
import no.nav.sikkerhet.authentication.AuthenticationResult;
import no.nav.sikkerhet.authentication.Authenticator;
import no.nav.sikkerhet.authentication.basic.BasicAuthenticator;
import no.nav.sikkerhet.authentication.basic.LdapConfiguration;
import no.nav.sikkerhet.authentication.oidc.OidcTokenValidator;
import no.nav.sikkerhet.authentication.saml.SAMLValidator;
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
import org.opensaml.core.config.InitializationException;
import org.opensaml.core.config.InitializationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import javax.sql.DataSource;
import java.time.Duration;
import java.util.logging.Level;

import static java.util.logging.Logger.getLogger;

public class SakApplication extends ResourceConfig {
    private static final Logger log = LoggerFactory.getLogger(SakApplication.class);

    @SuppressWarnings("WeakerAccess") //Påkrevd public
    public SakApplication() {
        DefaultExports.initialize();

        SakConfiguration sakConfiguration = new SakConfiguration();
        DataSource sakDataSource = createSakDataSource(sakConfiguration);


        migrateDataWarehouse(sakConfiguration);

        migrateSak(sakDataSource);


        Database database = createDatabase(sakDataSource);

        registerApiResources(database, sakConfiguration);
        registerFilters(sakConfiguration);
        registerExceptionmappers();
        registerFeatures();
        registerSwaggerResources();
        initSAML();

        log.info("Jersey-Application ferdig initialisert");
    }

    void migrateDataWarehouse(SakConfiguration sakConfiguration) {
        DataSource sakGrDataSource = createSakGrDataSource(sakConfiguration);
        migrateSakGr(sakGrDataSource);
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
    }

    private void registerFilters(SakConfiguration sakConfiguration) {
        register(new CorrelationFilter());
        register(new PrometheusFilter());
        registerAuthenticationFilter(sakConfiguration);
    }

    void registerApiResources(Database database, SakConfiguration sakConfiguration) {
        ABACClient abacClient = new ABACClient(sakConfiguration.getRequiredString("ABAC_PDP_ENDPOINT"), createHttpClient(sakConfiguration));
        register(new SakResource(
            new SakRepository(database),
            new SakPEP(abacClient, sakConfiguration))
        );
    }

    void registerAuthenticationFilter(SakConfiguration sakConfiguration) {
        HttpsJwks httpsJwks = new HttpsJwks(sakConfiguration.getRequiredString("OPENIDCONNECT_ISSO_JWKS"));
        OidcTokenValidator oidcTokenValidator = new OidcTokenValidator(new HttpsJwksVerificationKeyResolver(httpsJwks),
            sakConfiguration.getRequiredString("OPENIDCONNECT_ISSO_ISSUER"));

        SAMLValidator samlValidator = new SAMLValidator(
            sakConfiguration.getRequiredString("javax.net.ssl.trustStore"),
            sakConfiguration.getRequiredString("javax.net.ssl.trustStorePassword"));

        LdapConfiguration ldapConfiguration = new LdapConfiguration(
            sakConfiguration.getRequiredString("LDAP_SERVICEUSER_BASEDN"),
            sakConfiguration.getRequiredString("LDAP_URL"),
            sakConfiguration.getRequiredString("LDAP_USERNAME"),
            sakConfiguration.getString("LDAP_PASSWORD", null));

        CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
            .withCache("basicAuth",
                CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, AuthenticationResult.class, ResourcePoolsBuilder.heap(100))
                    .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofMinutes(5))))
            .build();
        cacheManager.init();

        Cache<String, AuthenticationResult> cache =
            cacheManager.getCache("basicAuth", String.class, AuthenticationResult.class);

        BasicAuthenticator basicAuthenticator = new BasicAuthenticator(ldapConfiguration);
        Authenticator authenticator = new Authenticator(oidcTokenValidator, samlValidator, basicAuthenticator);
        register(new AuthenticationFilter(authenticator));
    }

    private void initSAML() {
        try {
            InitializationService.initialize();
        } catch (InitializationException e) {
            throw new IllegalStateException("Feilet under initialisering av SAML", e);
        }
    }

    private HttpClient createHttpClient(SakConfiguration sakConfiguration) {
        int timeout = Integer.parseInt(sakConfiguration.getString("ABAC_READ_TIMEOUT", "5000"));
        RequestConfig requestConfig = RequestConfig
            .custom()
            .setConnectionRequestTimeout(timeout)
            .setSocketTimeout(timeout)
            .setConnectTimeout(timeout)
            .build();


        HttpClientBuilder httpClientBuilder = HttpClientBuilder
            .create()
            .setDefaultRequestConfig(requestConfig);

        if (sakConfiguration.getBoolean("ABAC_ENABLED", false)) {
            Username***passord=gammelt_passord***(
                sakConfiguration.getRequiredString("SRVSAK_USERNAME"),
                sakConfiguration.getRequiredString("SRVSAK_PASSWORD"));

            CredentialsProvider provider = new BasicCredentialsProvider();
            provider.setCredentials(AuthScope.ANY, credentials);
            httpClientBuilder.setDefaultCredentialsProvider(provider);
        }
        return httpClientBuilder.build();
    }

    private void registerSwaggerResources() {
        BeanConfig beanConfig = new BeanConfig();
        beanConfig.setVersion("v1");
        beanConfig.setBasePath("/api");
        beanConfig.setTitle("Sak API");
        beanConfig.setResourcePackage("no.nav.sak");
        beanConfig.setScan();
        register(ApiListingResource.class);
        register(SwaggerSerializers.class);

    }

    protected Database createDatabase(DataSource dataSource) {
        return new Database(dataSource);
    }

    void migrateSak(DataSource dataSource) {
        new FlywayMigrator(dataSource, "classpath:db/migration", "classpath:db/oracle/migration").migrate();
    }

    private void migrateSakGr(DataSource sakGrDataSource) {
        new FlywayMigrator(sakGrDataSource, "classpath:db/dvh/migration").migrate();
    }

    private void registerLoggingFeature() {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
        register(new LoggingFeature(getLogger(LoggingFeature.class.getName()), Level.INFO, LoggingFeature.Verbosity.PAYLOAD_TEXT, LoggingFeature.DEFAULT_MAX_ENTITY_SIZE));
    }


    protected DataSource createSakDataSource(SakConfiguration sakConfiguration) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setMetricsTrackerFactory(new PrometheusMetricsTrackerFactory());

        dataSource.setUsername(sakConfiguration.getRequiredString("SAKDS_USERNAME"));
        dataSource.setPassword(sakConfiguration.getRequiredString("SAKDS_PASSWORD"));
        dataSource.setJdbcUrl(sakConfiguration.getRequiredString("SAKDS_URL"));

        log.info("Opprettet datasource: {}", dataSource.getJdbcUrl());
        return dataSource;
    }

    private DataSource createSakGrDataSource(SakConfiguration sakConfiguration) {
        HikariDataSource dataSource = new HikariDataSource();

        dataSource.setUsername(sakConfiguration.getRequiredString("SAK_GR_DS_USERNAME"));
        dataSource.setPassword(sakConfiguration.getRequiredString("SAK_GR_DS_PASSWORD"));
        dataSource.setJdbcUrl(sakConfiguration.getRequiredString("SAK_GR_DS_URL"));

        log.info("Opprettet datasource for dvh: {}", dataSource.getJdbcUrl());
        return dataSource;
    }
}
