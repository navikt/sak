package no.nav.sak.infrastruktur.authentication;

import lombok.Getter;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class AuthenticationResult {
	@Getter
	private final String user;
	@Getter
	private final String consumerId;
	@Getter
	private final boolean isValid;
	private final String errorMessage;

	private AuthenticationResult(String user, String consumerId, boolean isValid, String errorMessage) {
		this.user = user;
		this.isValid = isValid;
		this.errorMessage = errorMessage;
		this.consumerId = consumerId;
	}

	public static AuthenticationResult invalid(String errorMessage) {
		return new AuthenticationResult(null, null, false, errorMessage);
	}

	public static AuthenticationResult success(String user, String consumerId) {
		return new AuthenticationResult(user, consumerId, true, null);
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
				.append("user", user)
				.append("consumerId", consumerId)
				.append("isValid", isValid)
				.append("errorMessage", errorMessage)
				.toString();
	}
}
