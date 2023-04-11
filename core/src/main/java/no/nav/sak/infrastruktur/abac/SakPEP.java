package no.nav.sak.infrastruktur.abac;

import io.vavr.CheckedFunction1;
import lombok.extern.slf4j.Slf4j;
import no.nav.abac.xacml.NavAttributter;
import no.nav.abac.xacml.StandardAttributter;
import no.nav.resilience.ResilienceConfig;
import no.nav.resilience.ResilienceExecutor;
import no.nav.sak.infrastruktur.SubjectType;
import no.nav.sak.infrastruktur.authentication.AuthenticationFilter;
import no.nav.sikkerhet.abac.ABACAttribute;
import no.nav.sikkerhet.abac.ABACCategory;
import no.nav.sikkerhet.abac.ABACClient;
import no.nav.sikkerhet.abac.ABACRequest;
import no.nav.sikkerhet.abac.ABACResult;

import javax.ws.rs.container.ContainerRequestContext;
import java.util.Objects;

import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static no.nav.abac.xacml.NavAttributter.ENVIRONMENT_FELLES_OIDC_TOKEN_BODY;
import static no.nav.abac.xacml.NavAttributter.ENVIRONMENT_FELLES_PEP_ID;
import static no.nav.abac.xacml.NavAttributter.ENVIRONMENT_FELLES_SAML_TOKEN;
import static no.nav.abac.xacml.NavAttributter.RESOURCE_FELLES_DOMENE;
import static no.nav.abac.xacml.NavAttributter.RESOURCE_FELLES_PERSON_AKTOERID_RESOURCE;
import static no.nav.abac.xacml.NavAttributter.RESOURCE_FELLES_RESOURCE_TYPE;
import static no.nav.sak.infrastruktur.abac.AbacExceptionTranslator.identifyException;
import static no.nav.sikkerhet.authentication.AuthenticationHeaderIdentifier.BASIC;
import static no.nav.sikkerhet.authentication.AuthenticationHeaderIdentifier.OIDC;
import static no.nav.sikkerhet.authentication.AuthenticationHeaderIdentifier.SAML;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.apache.commons.lang3.StringUtils.substringBetween;
import static org.apache.commons.lang3.StringUtils.trim;

@Slf4j
public class SakPEP {
	static final String RESOURCE_TYPE_SAK = "no.nav.abac.attributter.resource.sak.sak";

	private final ABACClient abacClient;
	private final ResilienceExecutor<ABACRequest, ABACResult> resilienceExecutor;

    public SakPEP(ABACClient abacClient, ResilienceConfig resilienceConfig) {
		this.abacClient = abacClient;
		final CheckedFunction1<ABACRequest, ABACResult> abacClientFunction = abacClient::execute;
		this.resilienceExecutor = new ResilienceExecutor<>(abacClientFunction, resilienceConfig);
	}

	public ABACResult autoriser(
			final ContainerRequestContext ctx
			, final AuthorizationRequest authorizationRequest) {

		ABACCategory category = new ABACCategory()
				.addAttribute(new ABACAttribute(RESOURCE_FELLES_DOMENE, "sak"))
				.addAttribute(new ABACAttribute(RESOURCE_FELLES_RESOURCE_TYPE, RESOURCE_TYPE_SAK));

		final ABACRequest abacRequest = ABACRequest.newRequest()
				.addEnvironment(new ABACAttribute(ENVIRONMENT_FELLES_PEP_ID, "sak"))
				.addResource(category);

		authorizationRequest.getAktoerId().ifPresent(aktoerId -> category.addAttribute(new ABACAttribute(RESOURCE_FELLES_PERSON_AKTOERID_RESOURCE, aktoerId)));

		final String authIdentifier = substringBefore(trim(ctx.getHeaderString(AUTHORIZATION)), " ");
		final String token = substringAfter(trim(ctx.getHeaderString(AUTHORIZATION)), " ");

		if (Objects.equals(BASIC.getValue(), authIdentifier)) {
			abacRequest.addAccessSubject(new ABACAttribute(StandardAttributter.SUBJECT_ID, (String) ctx.getProperty(AuthenticationFilter.REQUEST_USERNAME)));
			abacRequest.addAccessSubject(new ABACAttribute(NavAttributter.SUBJECT_FELLES_SUBJECTTYPE, SubjectType.SUBJECT_TYPE_SYSTEMBRUKER.getValue()));
		} else if (Objects.equals(OIDC.getValue(), authIdentifier)) {
			final String tokenBody = substringBetween(token, ".");
			abacRequest.addEnvironment(new ABACAttribute(ENVIRONMENT_FELLES_OIDC_TOKEN_BODY, tokenBody));
		} else if (Objects.equals(SAML.getValue(), authIdentifier)) {
			abacRequest.addEnvironment(new ABACAttribute(ENVIRONMENT_FELLES_SAML_TOKEN, token));
		} else {
			throw new IllegalStateException("Fant ingen gyldig authenticationHeader");
		}

		final ABACResult abacResult;
		try {
			abacResult = resilienceExecutor.execute(abacRequest);
			if (ABACResult.Code.OK.equals(abacResult.getResultCode())) {
				if (abacResult.hasAccess()) {
					log.info("Bruker fikk permit. ConsumerID: {}; User: {};",
							ctx.getProperty(AuthenticationFilter.REQUEST_CONSUMERID),
							ctx.getProperty(AuthenticationFilter.REQUEST_USERNAME));
				} else {
					log.info("Bruker fikk deny. ConsumerID: {}; User: {};",
							ctx.getProperty(AuthenticationFilter.REQUEST_CONSUMERID),
							ctx.getProperty(AuthenticationFilter.REQUEST_USERNAME));
				}
			}
		} catch (Throwable t) {
			throw identifyException(t);
		}
		return abacResult;
	}
}
