package no.nav.sak.infrastruktur.authentication;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import lombok.Value;

@Value
@ToString
@Getter(AccessLevel.PACKAGE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PUBLIC, builderClassName = "LdapConfigurationBuilder", setterPrefix = "with")
public class LdapConfiguration {

	@NonNull
	String serviceUserBaseDN;
	@NonNull
	String url;
	@NonNull
	String bindUser;
	@ToString.Exclude
	String bindPassword;

	@SuppressWarnings("unused")
	public static class LdapConfigurationBuilder {
		public LdapConfiguration build() {
			LdapConfigurationValidator.validate(serviceUserBaseDN, url, bindUser, bindPassword);
			return new LdapConfiguration(serviceUserBaseDN, url, bindUser, bindPassword);
		}
	}
}