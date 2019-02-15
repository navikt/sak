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
import no.nav.sikkerhet.resilience.ResilienceConfig;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import javax.sql.DataSource;
import java.time.Duration;
import java.util.*;
import java.util.logging.Level;

import static java.util.logging.Logger.getLogger;

public class SakApplication extends ResourceConfig {

    private static final Logger log = LoggerFactory.getLogger(SakApplication.class);

    private static final long RESIL_CFG_DOWNSTREAMS_CALL_TIMEOUT_IN_MILLISECONDS =
        ResilienceConfig.DEFAULT_RESIL_CFG_DOWNSTREAMS_CALL_TIMEOUT_IN_MILLISECONDS;
    private static final int RESIL_CFG_NUMBER_OF_RETRIES_UPON_EXCEPTION =
        ResilienceConfig.DEFAULT_RESIL_CFG_NUMBER_OF_RETRIES_UPON_EXCEPTION;
    private static final long RESIL_CFG_WAIT_DURATION_IN_MILLISECONDS_BETWEEEN_RETRIES =
        ResilienceConfig.DEFAULT_RESIL_CFG_WAIT_DURATION_IN_MILLISECONDS_BETWEEEN_RETRIES;
    private static final int RESIL_CFG_RING_BUFFER_SIZE_IN_CLOSED_STATE = 13;
    private static final int RESIL_CFG_RING_BUFFER_SIZE_IN_HALF_OPEN_STATE = 5;
    private static final float RESIL_CFG_FAILURE_RATE_THRESHOLD_PERCENTAGE = 50.F;
    private static final long RESIL_CFG_WAIT_DURATIONIN_IN_MILLISECONDS_IN_OPEN_STATE = 30000L;

    @SuppressWarnings("WeakerAccess") //Påkrevd public
    public SakApplication() {

        DefaultExports.initialize();

        final SakConfiguration sakConfiguration = new SakConfiguration();
        final DataSource sakDataSource = createSakDataSource(sakConfiguration);

        migrateDataWarehouse(sakConfiguration);

        migrateSak(sakDataSource);

        final Database database = createDatabase(sakDataSource);

        registerApiResources(database, sakConfiguration);
        registerFilters(sakConfiguration);
        registerExceptionmappers();
        registerFeatures();
        registerSwaggerResources();
        initSAML();

        log.info("Jersey-Application ferdig initialisert");
    }

    void migrateDataWarehouse(final SakConfiguration sakConfiguration) {

        if(sakConfiguration.getBoolean("MIGRATE_DVH", true)) {
            DataSource sakGrDataSource = createSakGrDataSource(sakConfiguration);
            migrateSakGr(sakGrDataSource);
        } else {
            log.warn("Datavarehus-migrering er skrudd av - ev. endringer i skjema vil ikke migreres");
        }
    }

    void registerApiResources(final Database database, final SakConfiguration sakConfiguration) {

        final ResilienceConfig resilienceConfig = createResilienceConfig();
        ABACClient abacClient;
        if(sakConfiguration.getBoolean("RESILIENCE_ENABLED", false)) {
            abacClient =  new ABACClient(
                    sakConfiguration.getRequiredString("ABAC_PDP_ENDPOINT"),
                    createHttpClient(sakConfiguration),
                    resilienceConfig);
        } else {
            abacClient =  new ABACClient(
                sakConfiguration.getRequiredString("ABAC_PDP_ENDPOINT"),
                createHttpClient(sakConfiguration));
        }


        register(new SakResource(
            new SakRepository(database),
            new SakPEP(abacClient))
        );
    }

    void registerAuthenticationFilter(final SakConfiguration sakConfiguration) {
        Map<String, VerificationKeyResolver> resolvers = new HashMap<>();
        List<OIDCIssuer> supportedIssuers = Collections.singletonList(
            new OIDCIssuer(sakConfiguration.getRequiredString("OPENIDCONNECT_ISSO_ISSUER"), sakConfiguration.getRequiredString("OPENIDCONNECT_ISSO_JWKS"))
        );

        for(OIDCIssuer issuer: supportedIssuers) {
            resolvers.put(issuer.issuer, new HttpsJwksVerificationKeyResolver(new HttpsJwks(issuer.jwks)));
        }


        final OidcTokenValidator oidcTokenValidator = new OidcTokenValidator(resolvers);

        final SAMLValidator samlValidator = new SAMLValidator(
            sakConfiguration.getRequiredString("javax.net.ssl.trustStore"),
            sakConfiguration.getRequiredString("javax.net.ssl.trustStorePassword"));

        final LdapConfiguration ldapConfiguration = new LdapConfiguration(
            sakConfiguration.getRequiredString("LDAP_SERVICEUSER_BASEDN"),
            sakConfiguration.getRequiredString("LDAP_URL"),
            sakConfiguration.getRequiredString("LDAP_USERNAME"),
            sakConfiguration.getString("LDAP_PASSWORD", null));

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
        register(new AuthenticationFilter(authenticator));
    }

    void migrateSak(final DataSource dataSource) {
        new FlywayMigrator(dataSource, "classpath:db/migration", "classpath:db/oracle/migration").migrate();
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
    }

    private void registerFilters(final SakConfiguration sakConfiguration) {
        register(new CorrelationFilter());
        register(new PrometheusFilter());
        registerAuthenticationFilter(sakConfiguration);
    }

    private ResilienceConfig createResilienceConfig() {
        return
            new ResilienceConfig.Builder()
                .withDownstreamsCallTimeoutInMilliseconds(RESIL_CFG_DOWNSTREAMS_CALL_TIMEOUT_IN_MILLISECONDS)
                .withNumberOfRetriesUponException(RESIL_CFG_NUMBER_OF_RETRIES_UPON_EXCEPTION)
                .withWaitDurationInMillisecondsBetweeenRetries(RESIL_CFG_WAIT_DURATION_IN_MILLISECONDS_BETWEEEN_RETRIES)
                .withRingBufferSizeInClosedState(RESIL_CFG_RING_BUFFER_SIZE_IN_CLOSED_STATE)
                .withRingBufferSizeInHalfOpenState(RESIL_CFG_RING_BUFFER_SIZE_IN_HALF_OPEN_STATE)
                .withExceptionRateBeforeOpeningTheCircuitBreaker(RESIL_CFG_FAILURE_RATE_THRESHOLD_PERCENTAGE)
                .withWaitDurationInMillisecondsInOpenState(RESIL_CFG_WAIT_DURATIONIN_IN_MILLISECONDS_IN_OPEN_STATE)
                .build();
    }

    private void initSAML() {
        try {
            InitializationService.initialize();
        } catch (InitializationException e) {
            throw new IllegalStateException("Feilet under initialisering av SAML", e);
        }
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

        final Username***passord=gammelt_passord***(
            sakConfiguration.getRequiredString("SRVSAK_USERNAME"),
            sakConfiguration.getRequiredString("SRVSAK_PASSWORD"));

        final CredentialsProvider provider = new BasicCredentialsProvider();
        provider.setCredentials(AuthScope.ANY, credentials);
        httpClientBuilder.setDefaultCredentialsProvider(provider);

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

    private void migrateSakGr(final DataSource sakGrDataSource) {
        new FlywayMigrator(sakGrDataSource, "classpath:db/dvh/migration").migrate();
    }

    private void registerLoggingFeature() {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
        register(new LoggingFeature(getLogger(LoggingFeature.class.getName()), Level.INFO, LoggingFeature.Verbosity.PAYLOAD_TEXT, LoggingFeature.DEFAULT_MAX_ENTITY_SIZE));
    }


    protected DataSource createSakDataSource(final SakConfiguration sakConfiguration) {

        final HikariDataSource dataSource = new HikariDataSource();
        dataSource.setMetricsTrackerFactory(new PrometheusMetricsTrackerFactory());

        dataSource.setUsername(sakConfiguration.getRequiredString("SAKDS_USERNAME"));
        dataSource.setPassword(sakConfiguration.getRequiredString("SAKDS_PASSWORD"));
        dataSource.setJdbcUrl(sakConfiguration.getRequiredString("SAKDS_URL"));

        log.info("Opprettet datasource: {}", dataSource.getJdbcUrl());
        return dataSource;
    }

    private DataSource createSakGrDataSource(final SakConfiguration sakConfiguration) {

        final HikariDataSource dataSource = new HikariDataSource();

        dataSource.setUsername(sakConfiguration.getRequiredString("SAK_GR_DS_USERNAME"));
        dataSource.setPassword(sakConfiguration.getRequiredString("SAK_GR_DS_PASSWORD"));
        dataSource.setJdbcUrl(sakConfiguration.getRequiredString("SAK_GR_DS_URL"));

        log.info("Opprettet datasource for dvh: {}", dataSource.getJdbcUrl());

        return dataSource;
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
