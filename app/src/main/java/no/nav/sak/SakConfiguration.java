package no.nav.sak;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import lombok.extern.slf4j.Slf4j;
import no.nav.resilience.ResilienceConfig;
import no.nav.sak.configuration.LdapProperties;
import no.nav.sak.configuration.SakProperties;
import no.nav.sak.configuration.ServiceuserProperties;
import no.nav.sak.infrastruktur.authentication.AuthenticationResult;
import no.nav.sak.infrastruktur.authentication.Authenticator;
import no.nav.sak.infrastruktur.authentication.BasicAuthenticator;
import no.nav.sak.infrastruktur.authentication.LdapConfiguration;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;

@Slf4j
@EnableAutoConfiguration
@Configuration
@EnableConfigurationProperties({
		SakProperties.class,
		LdapProperties.class,
		ServiceuserProperties.class
})
public class SakConfiguration {

	public static final ZoneId NORWAY_TIME = ZoneId.of("Europe/Oslo");

	@Bean
	public Clock clock() {
		return Clock.system(NORWAY_TIME);
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
	public BasicAuthenticator basicAuthenticator(SakProperties sakProperties,
												 LdapConfiguration ldapConfiguration,
												 Cache<String, AuthenticationResult> cache) {
		return new BasicAuthenticator(sakProperties, ldapConfiguration, cache);
	}

	@Bean
	public Authenticator authenticator(BasicAuthenticator basicAuthenticator) {
		return new Authenticator(basicAuthenticator);
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
