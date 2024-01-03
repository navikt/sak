package no.nav.sak.infrastruktur.abac;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

public class ABACAdvice {
	@JsonProperty("Id")
	private String id;
	@JsonProperty("AttributeAssignment")
	private Collection<Map<String, String>> attributeAssignment;

	public String getId() {
		return id;
	}

	public Collection<Map<String, String>> getAttributeAssignment() {
		if (attributeAssignment == null) {
			return new ArrayList<>();
		}
		return attributeAssignment;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this)
				.append("id", id)
				.append("attributeAssignment", attributeAssignment)
				.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ABACAdvice that = (ABACAdvice) o;
		return Objects.equals(id, that.id) &&
				Objects.equals(attributeAssignment, that.attributeAssignment);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, attributeAssignment);
	}
}
