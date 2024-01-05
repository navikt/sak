package no.nav.sak.infrastruktur.abac;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Objects;

@JsonIgnoreProperties(value = {"Status"})
@JsonRootName("Response")
public class ABACResponse {

	@JsonProperty("Decision")
	private String decision;

	@JsonProperty("AssociatedAdvice")
	private ABACAdvice associatedAdvice;

	@JsonProperty("Category")
	private ABACCategory category;

	public String getDecision() {
		return decision;
	}

	public ABACAdvice getAssociatedAdvice() {
		if (associatedAdvice == null) {
			return new ABACAdvice();
		}
		return associatedAdvice;
	}

	public ABACCategory getCategory() {
		return category;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this)
				.append("decision", decision)
				.append("associatedAdvice", associatedAdvice)
				.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ABACResponse that = (ABACResponse) o;
		return Objects.equals(decision, that.decision) &&
				Objects.equals(associatedAdvice, that.associatedAdvice);
	}

	@Override
	public int hashCode() {
		return Objects.hash(decision, associatedAdvice);
	}
}