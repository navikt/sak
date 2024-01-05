package no.nav.sak.infrastruktur.abac;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Objects;

import static org.apache.commons.lang3.StringUtils.removePattern;

@JsonIgnoreProperties(value = {"DataType"})
public class ABACAttribute {

	@JsonProperty("AttributeId")
	private String attributeId;

	@JsonProperty("Value")
	private String value;

	@JsonProperty("IncludeInResult")
	private Boolean includeInResult;

	public ABACAttribute() {
	}

	public ABACAttribute(final String attributeId, final String value) {
		this(attributeId, value, false);
	}

	public ABACAttribute(final String attributeId, final String value, Boolean includeInResult) {
		this.attributeId = attributeId;
		this.value = value;
		this.includeInResult = includeInResult;
	}

	public String getAttributeId() {
		return attributeId;
	}

	public String getValue() {
		return value;
	}

	Boolean isIncludeInResult() {
		return includeInResult;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this, ToStringStyle.NO_CLASS_NAME_STYLE)
				.append(removePattern(attributeId, ".*\\."), removePattern(value, ".*\\."))
				.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ABACAttribute that = (ABACAttribute) o;
		return Objects.equals(attributeId, that.attributeId) &&
				Objects.equals(value, that.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(attributeId, value);
	}
}