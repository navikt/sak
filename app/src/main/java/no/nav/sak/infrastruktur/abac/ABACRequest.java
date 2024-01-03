package no.nav.sak.infrastruktur.abac;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@JsonRootName("Request")
public class ABACRequest {

	public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	static {
		OBJECT_MAPPER.enable(SerializationFeature.WRAP_ROOT_VALUE);
	}

	@JsonProperty("Environment")
	private ABACCategory environment = new ABACCategory();
	@JsonProperty("Resource")
	private List<ABACCategory> resources = new ArrayList<>();
	@JsonProperty("Action")
	private ABACCategory action = new ABACCategory();
	@JsonProperty("AccessSubject")
	private ABACCategory accessSubject = new ABACCategory();

	public static ABACRequest newRequest() {
		return new ABACRequest();
	}

	public ABACRequest addEnvironment(ABACAttribute attribute) {
		getEnvironment().getAttributes().add(attribute);
		return this;
	}

	public ABACRequest addResource(ABACCategory resource) {
		getResources().add(resource);
		return this;
	}

	public ABACRequest addAction(ABACAttribute attribute) {
		getAction().getAttributes().add(attribute);
		return this;
	}

	public ABACRequest addAccessSubject(ABACAttribute attribute) {
		getAccessSubject().getAttributes().add(attribute);
		return this;
	}

	public String toJSON() {
		try {
			return OBJECT_MAPPER.writeValueAsString(this);
		} catch (JsonProcessingException e) {
			throw new IllegalArgumentException("Failed to serialize ABAC request to json", e);
		}

	}

	public ABACCategory getEnvironment() {
		return environment;
	}

	public List<ABACCategory> getResources() {
		return resources;
	}

	public ABACCategory getAction() {
		return action;
	}

	public ABACCategory getAccessSubject() {
		return accessSubject;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
				.append("resource", resources)
				.append("action", action)
				.append("accessSubject", accessSubject)
				.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ABACRequest that = (ABACRequest) o;
		return Objects.equals(environment, that.environment) &&
				Objects.equals(resources, that.resources) &&
				Objects.equals(action, that.action) &&
				Objects.equals(accessSubject, that.accessSubject);
	}

	@Override
	public int hashCode() {
		return Objects.hash(environment, resources, action, accessSubject);
	}
}