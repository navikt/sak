package no.nav.sak.infrastruktur.abac;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.Validate;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class ABACClient {
	public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	static {
		OBJECT_MAPPER.enable(DeserializationFeature.UNWRAP_ROOT_VALUE);
	}

	private static final Logger log = LoggerFactory.getLogger(ABACClient.class);
	private static final String MEDIATYPE_APPLICATION_XACML_JSON = "application/xacml+json";
	private final String endpoint;
	private final HttpClient httpClient;


	/**
	 * @param endpoint   Must not be <code>null</code>
	 * @param httpClient Must not be <code>null</code>
	 */
	public ABACClient(final String endpoint, final HttpClient httpClient) {
		Validate.notNull(endpoint, "The endpoint must not be null");
		Validate.notNull(httpClient, "The httpClient must not be null");
		this.httpClient = httpClient;
		this.endpoint = endpoint;
	}

	/**
	 * @param abacRequest
	 * @return ABACResult reflecting success or the contrary
	 */
	public ABACResult execute(final ABACRequest abacRequest) {
		return executeForAny(abacRequest, this::makeABACResult, ABACResult.class);
	}

	private <ANY_ABAC_RESULT_TYPE> ANY_ABAC_RESULT_TYPE executeForAny(
			final ABACRequest abacRequest,
			final Function<HttpResponse, ANY_ABAC_RESULT_TYPE> producerOfAnyABACResultFromHttpResponse,
			final Class<ANY_ABAC_RESULT_TYPE> anyAbacResultClazz) {

		try {
			final HttpPost httpPost = makeHttpPost(this.endpoint, abacRequest);
			final HttpResponse httpResponse = executeDownstreams(httpPost);
			checkHttpResponseInterpretability(httpResponse);
			return producerOfAnyABACResultFromHttpResponse.apply(httpResponse);
		} catch (RuntimeException e) {
			log.error("Exception received when calling downstreams to ABAC.", e);
			throw e;
		}
	}

	HttpPost makeHttpPost(final String endpoint, final ABACRequest abacRequest) {
		final HttpPost httpPost = newHttpPost(endpoint);
		httpPost.addHeader(HttpHeaders.CONTENT_TYPE, MEDIATYPE_APPLICATION_XACML_JSON);
		try {
			final StringEntity stringEntity = newStringEntity(abacRequest);
			httpPost.setEntity(stringEntity);
			return httpPost;
		} catch (UnsupportedEncodingException e) {
			final String msg = "Failed while creating ABAC request";
			log.error(msg);
			throw new IllegalStateException(msg, e);
		}
	}

	HttpPost newHttpPost(final String endpoint) {
		return new HttpPost(endpoint);
	}

	StringEntity newStringEntity(final ABACRequest abacRequest) throws UnsupportedEncodingException {
		return new StringEntity(makeJSONFromAabacRequest(abacRequest));
	}

	private String makeJSONFromAabacRequest(final ABACRequest abacRequest) {
		return abacRequest.toJSON();
	}

	HttpResponse executeDownstreams(final HttpPost httpPost) {
		try {
			return httpClient.execute(httpPost);
		} catch (IOException e) {
			log.error("Failed to send ABAC request", e);
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	void checkHttpResponseInterpretability(final HttpResponse httpResponse) {
		final StatusLine statusLine = httpResponse.getStatusLine();
		final int statusCode = statusLine.getStatusCode();
		String errorMsg = null;
		IllegalStateException exception = null;
		try {
			if (Response.Status.OK.getStatusCode() != statusCode) {
				final String responseBody = EntityUtils.toString(httpResponse.getEntity());
				errorMsg = format("Downstreams call to ABAC resulted in an error response. statusLine: %s, responseBody: %s", statusLine, responseBody);
				exception = new IllegalStateException(errorMsg);
			}
		} catch (IOException e) {
			errorMsg = "Failed to read http response";
			exception = new IllegalStateException(errorMsg, e);
		} finally {
			if (exception != null) {
				log.error(errorMsg, exception);
				throw exception;
			}
		}
	}

	private ABACResult makeABACResult(final HttpResponse httpResponse) {
		final ABACResponse abacResponse = makeABACResponse(httpResponse);
		return new ABACResult(abacResponse.getDecision(), abacResponse.getAssociatedAdvice()
				.getAttributeAssignment()
				.stream()
				.map(v -> new ABACAttribute(v.get("AttributeId"), v.get("Value")))
				.collect(Collectors.toList()));
	}

	ABACResponse makeABACResponse(final HttpResponse httpResponse) {
		final HttpEntity httpEntity = httpResponse.getEntity();

		try {
			final InputStream httpEntityInputStream = httpEntity.getContent();
			return OBJECT_MAPPER.readValue(httpEntityInputStream, ABACResponse.class);
		} catch (IOException e) {
			final String errorMsg = "Failed to deserialize response from ABAC";
			throw new IllegalArgumentException(errorMsg, e);
		}
	}

}
