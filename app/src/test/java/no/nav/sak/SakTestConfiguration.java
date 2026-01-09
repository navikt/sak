package no.nav.sak;

import no.nav.resilience.ResilienceConfig;
import no.nav.sak.configuration.AbacProperties;
import no.nav.sak.configuration.LdapProperties;
import no.nav.sak.configuration.ServiceuserProperties;
import no.nav.sak.infrastruktur.abac.ABACClient;
import no.nav.sak.infrastruktur.abac.ABACJunitClient;
import no.nav.sak.infrastruktur.abac.MockableSakPEP;
import no.nav.sak.infrastruktur.authentication.Authenticator;
import no.nav.sak.infrastruktur.authentication.LdapConfiguration;
import no.nav.sak.infrastruktur.authentication.basic.JunitBasicAuthenticator;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@EnableConfigurationProperties({
		AbacProperties.class,
		LdapProperties.class,
		ServiceuserProperties.class
})
@ComponentScan(basePackages = "no.nav.sak")
public class SakTestConfiguration {

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
		LdapConfiguration ldapConfiguration = LdapConfiguration.builder()
				.withUrl(ldapProperties.url())
				.withServiceUserBaseDN(ldapProperties.serviceuserBasedn())
				.withBindUser(ldapProperties.username())
				.withBindPassword(null)
				.build();

		JunitBasicAuthenticator junitBasicAuthenticator = new JunitBasicAuthenticator(ldapConfiguration);

		return new Authenticator(junitBasicAuthenticator);
	}
}
