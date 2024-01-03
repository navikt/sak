package no.nav.sak.infrastruktur.abac;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.List;
import java.util.Objects;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;

public class ABACResult {

	public enum Code {
		OK("An ABAC call was carried out without any known problems."),
		INVALID("Some exception has been thrown from an unknown level, and has not been taken specifically care of.");

		private final String description;

		Code(final String description) {
			this.description = description;
		}

		public String getDescription() {
			return description;
		}
	}

	private final ABACResult.Code resultCode;

	private final String decision;
	private final List<ABACAttribute> associatedAdvice;

	public ABACResult(final String decision, final List<ABACAttribute> associatedAdvice) {
		this(ABACResult.Code.OK, decision, associatedAdvice);
	}

	public ABACResult(final ABACResult.Code resultCode, final String decision, final List<ABACAttribute> associatedAdvice) {
		if (ABACResult.Code.OK.equals(resultCode) && ((decision == null) || (associatedAdvice == null))) {
			throw new IllegalArgumentException("An ABACResult with an ABACResult.Code.OK must have decision and associatedAdvice different from null");
		} else if (!ABACResult.Code.OK.equals(resultCode) && ((decision != null) || (associatedAdvice != null))) {
			throw new IllegalArgumentException("An ABACResult with an ABACResult.Code different from ABACResult.Code.OK cannot have decision and/or associatedAdvice different from null");
		} else {
			this.resultCode = resultCode;
			this.decision = decision;
			this.associatedAdvice = associatedAdvice == null ? emptyList() : unmodifiableList(associatedAdvice);
		}
	}

	public boolean hasAccess() {
		return Decision.PERMIT.getValue().equals(this.decision);
	}

	public List<ABACAttribute> getAssociatedAdvice() {
		return this.associatedAdvice;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this, ToStringStyle.NO_CLASS_NAME_STYLE)
				.append("decision", this.decision)
				.append("associatedAdvice", this.associatedAdvice)
				.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ABACResult that = (ABACResult) o;
		return Objects.equals(this.decision, that.decision) &&
				Objects.equals(this.associatedAdvice, that.associatedAdvice);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.decision, this.associatedAdvice);
	}

	public Code getResultCode() {
		return resultCode;
	}
}