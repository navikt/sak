package no.nav.sak.infrastruktur.abac;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

@JsonIgnoreProperties(value = {"CategoryId"})
public class ABACCategory {

	private Collection<ABACAttribute> attributes = new ArrayList<>();

	@JsonProperty("Attribute")
	public Collection<ABACAttribute> getAttributes() {
		return attributes;
	}

	public ABACCategory addAttribute(ABACAttribute abacAttribute) {

		Validate.notNull(abacAttribute, "abacAttribute cannot be null");

		attributes.add(abacAttribute);

		return this;
	}

	void setAttributes(Collection<ABACAttribute> attributes) {

		Validate.notNull(attributes, "abacAttributes cannot be null");

		this.attributes = attributes;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
				.append("attributes", attributes)
				.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ABACCategory that = (ABACCategory) o;
		return Objects.equals(attributes, that.attributes);
	}

	@Override
	public int hashCode() {
		return Objects.hash(attributes);
	}
}