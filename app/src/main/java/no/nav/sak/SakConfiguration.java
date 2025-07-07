package no.nav.sak;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import lombok.extern.slf4j.Slf4j;
import no.nav.resilience.ResilienceConfig;
import no.nav.sak.configuration.AbacProperties;
import no.nav.sak.configuration.LdapProperties;
import no.nav.sak.configuration.ServiceuserProperties;
import no.nav.sak.configuration.StsProperties;
import no.nav.sak.infrastruktur.abac.ABACClient;
import no.nav.sak.infrastruktur.abac.SakPEP;
import no.nav.sak.infrastruktur.authentication.AuthenticationResult;
import no.nav.sak.infrastruktur.authentication.Authenticator;
import no.nav.sak.infrastruktur.authentication.BasicAuthenticator;
import no.nav.sak.infrastruktur.authentication.LdapConfiguration;
import no.nav.sak.infrastruktur.authentication.OidcTokenValidator;
import no.nav.sak.repository.Database;
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
import org.jose4j.jwk.HttpsJwks;
import org.jose4j.keys.resolvers.HttpsJwksVerificationKeyResolver;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;
import java.util.Map;

@Slf4j
@EnableAutoConfiguration
@Configuration
@EnableConfigurationProperties({
		AbacProperties.class,
		LdapProperties.class,
		ServiceuserProperties.class,
		StsProperties.class
})
public class SakConfiguration {

	public static final ZoneId NORWAY_TIME = ZoneId.of("Europe/Oslo");

	@Bean
	public Clock clock() {
		return Clock.system(NORWAY_TIME);
	}

	@Bean
	public SakPEP sakPEP(ABACClient abacClient, ResilienceConfig resilienceConfig) {
		return new SakPEP(abacClient, resilienceConfig);
	}

	@Bean
	public ABACClient createAbacClient(ServiceuserProperties serviceuserProperties, AbacProperties abacProperties) {
		return new ABACClient(
				abacProperties.endpoint(),
				createHttpClient(serviceuserProperties, abacProperties));
	}

	private HttpClient createHttpClient(ServiceuserProperties serviceuserProperties, AbacProperties abacProperties) {

		final int timeout = abacProperties.readTimeout();
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
				serviceuserProperties.username(),
				serviceuserProperties.password());

		final CredentialsProvider provider = new BasicCredentialsProvider();
		provider.setCredentials(AuthScope.ANY, credentials);
		httpClientBuilder.setDefaultCredentialsProvider(provider);

		return httpClientBuilder.build();
	}

	@Bean
	public OidcTokenValidator oidcTokenValidator(StsProperties stsProperties) {
		return new OidcTokenValidator(Map.of(stsProperties.issuer(), new HttpsJwksVerificationKeyResolver(new HttpsJwks(stsProperties.jwks()))));
	}

	@Bean
	public LdapConfiguration ldapConfiguration(LdapProperties ldapProperties) {
		return LdapConfiguration.builder()
				.withUrl(ldapProperties.url())
				.withBindUser(ldapProperties.username())
				.withBindPassword(ldapProperties.password())
				.withServiceUserBaseDN(ldapProperties.serviceuserBasedn())
				.build();
	}

	@Bean
	public Cache<String, AuthenticationResult> cache() {
		final CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
				.withCache("basicAuth",
						CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, AuthenticationResult.class, ResourcePoolsBuilder.heap(100))
								.withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofMinutes(5))))
				.build();

		cacheManager.init();
		return cacheManager.getCache("basicAuth", String.class, AuthenticationResult.class);
	}

	@Bean
	public BasicAuthenticator basicAuthenticator(LdapConfiguration ldapConfiguration, Cache<String, AuthenticationResult> cache) {
		return new BasicAuthenticator(ldapConfiguration, cache);
	}

	@Bean
	public Authenticator authenticator(OidcTokenValidator oidcTokenValidator, BasicAuthenticator basicAuthenticator) {
		return new Authenticator(oidcTokenValidator, basicAuthenticator);
	}

	@Bean
	public Database createDatabase(DataSource dataSource) {
		return new Database(dataSource);
	}

	@Bean
	public OpenAPI configureOpenApi() {
		return new OpenAPI()
				.info(
						new Info()
								.title("Sak API")
								.version("v1")
				);
	}

	@Bean
	public ResilienceConfig resilienceConfig() {
		return ResilienceConfig.ofDefaults();
	}

}
