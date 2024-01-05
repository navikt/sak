package no.nav.sak.infrastruktur.authentication;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LdapConfigurationValidator {

	private static final Logger log = LoggerFactory.getLogger(LdapConfigurationValidator.class);
	private static final String URL_REGEX =
			"\\b(ldaps|ldap)://"
					+ "[-A-Za-z0-9+&@#/%?=~_|!:,.;]"
					+ "*[-A-Za-z0-9+&@#/%=~_|]";

	static void validate(
			final String serviceUserBaseDN,
			final String url,
			final String bindUser,
			final String bindPassword
	) {
		Validate.notBlank(serviceUserBaseDN, "serviceUserBaseDN cannot be blank");
		validLdapUrl(url, "url is null, blank or not valid: \"%s\"", url);
		Validate.notBlank(bindUser, "bindUser cannot be blank");
		nullOrNotBlank(bindPassword, "bindPassword cannot be blank");
		if (bindPassword == null) {
			log.warn("Returning an LdapConfiguration instance with no password.");
		}
	}

	private static <T extends CharSequence> T nullOrNotBlank(final T value, final String message, Object... values) {
		if (value != null) {
			Validate.notBlank(value, message, values);
		}
		return value;
	}

	private static String validLdapUrl(
			final String value,
			final String message,
			final Object... values) {
		Validate.notBlank(value, message, values);
		if (!value.matches(URL_REGEX)) {
			throw new IllegalArgumentException(String.format(message, values));
		}
		return value;
	}
}