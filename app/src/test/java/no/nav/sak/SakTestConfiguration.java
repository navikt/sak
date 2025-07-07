package no.nav.sak;

import no.nav.resilience.ResilienceConfig;
import no.nav.sak.configuration.AbacProperties;
import no.nav.sak.configuration.LdapProperties;
import no.nav.sak.configuration.ServiceuserProperties;
import no.nav.sak.configuration.StsProperties;
import no.nav.sak.infrastruktur.abac.ABACClient;
import no.nav.sak.infrastruktur.abac.ABACJunitClient;
import no.nav.sak.infrastruktur.abac.MockableSakPEP;
import no.nav.sak.infrastruktur.authentication.Authenticator;
import no.nav.sak.infrastruktur.authentication.LdapConfiguration;
import no.nav.sak.infrastruktur.authentication.OidcTokenValidator;
import no.nav.sak.infrastruktur.authentication.basic.JunitBasicAuthenticator;
import no.nav.sak.infrastruktur.oicd.JunitJsonWebKey;
import no.nav.sak.infrastruktur.oicd.JwtClaimsTestData;
import org.jose4j.keys.resolvers.JwksVerificationKeyResolver;
import org.jose4j.keys.resolvers.VerificationKeyResolver;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.singletonList;

@Configuration
@EnableConfigurationProperties({
		AbacProperties.class,
		LdapProperties.class,
		ServiceuserProperties.class,
		StsProperties.class,
		SakTestTruststoreProperties.class
})
@ComponentScan(basePackages = "no.nav.sak")
public class SakTestConfiguration {

	@Bean
	public PlatformTransactionManager platformTransactionManager(DataSource dataSource) {
		return new DataSourceTransactionManager(dataSource);
	}

	@Bean
	@Primary
	public MockableSakPEP mockableSakPEP(ABACClient abacClient) {
		return new MockableSakPEP(abacClient, ResilienceConfig.ofDefaults());
	}

	@Bean
	@Primary
	protected ABACClient abacClient() {
		return ABACJunitClient.create();
	}

	@Bean
	@Primary
	public Authenticator testAuthenticator(LdapProperties ldapProperties) {
		Map<String, VerificationKeyResolver> verificationKeyResolverMap = new HashMap<>();
		verificationKeyResolverMap.put(JwtClaimsTestData.ISSUER, new JwksVerificationKeyResolver(
				singletonList(JunitJsonWebKey.get())));
		OidcTokenValidator oidcTokenValidator = new OidcTokenValidator(verificationKeyResolverMap);

		LdapConfiguration ldapConfiguration = LdapConfiguration.builder()
				.withUrl(ldapProperties.url())
				.withServiceUserBaseDN(ldapProperties.serviceuserBasedn())
				.withBindUser(ldapProperties.username())
				.withBindPassword(null)
				.build();

		JunitBasicAuthenticator junitBasicAuthenticator = new JunitBasicAuthenticator(ldapConfiguration);

		return new Authenticator(oidcTokenValidator, junitBasicAuthenticator);
	}
}
