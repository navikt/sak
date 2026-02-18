package no.nav.sak;

import no.nav.sak.configuration.LdapProperties;
import no.nav.sak.configuration.SakProperties;
import no.nav.sak.configuration.ServiceuserProperties;
import no.nav.sak.infrastruktur.authentication.Authenticator;
import no.nav.sak.infrastruktur.authentication.LdapConfiguration;
import no.nav.sak.infrastruktur.authentication.basic.JunitBasicAuthenticator;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@AutoConfigureTestRestTemplate
@Configuration
@EnableConfigurationProperties({
		SakProperties.class,
		LdapProperties.class,
		ServiceuserProperties.class
})
@ComponentScan(basePackages = "no.nav.sak")
public class SakTestConfiguration {

	@Bean
	@Primary
	public Authenticator testAuthenticator(SakProperties sakProperties, LdapProperties ldapProperties) {
		LdapConfiguration ldapConfiguration = LdapConfiguration.builder()
				.withUrl(ldapProperties.url())
				.withServiceUserBaseDN(ldapProperties.serviceuserBasedn())
				.withBindUser(ldapProperties.username())
				.withBindPassword(null)
				.build();

		JunitBasicAuthenticator junitBasicAuthenticator = new JunitBasicAuthenticator(sakProperties, ldapConfiguration);

		return new Authenticator(junitBasicAuthenticator);
	}
}
