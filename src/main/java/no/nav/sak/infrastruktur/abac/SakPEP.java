package no.nav.sak.infrastruktur.abac;

import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
import no.nav.abac.xacml.NavAttributter;
import no.nav.abac.xacml.StandardAttributter;
import no.nav.sak.infrastruktur.ContextExtractor;
import no.nav.sikkerhet.abac.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.ContainerRequestContext;
import java.util.Objects;

import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static no.nav.abac.xacml.NavAttributter.*;
import static no.nav.sak.infrastruktur.SubjectType.SUBJECT_TYPE_SYSTEMBRUKER;
import static no.nav.sak.infrastruktur.authentication.AuthenticationFilter.REQUEST_CONSUMERID;
import static no.nav.sak.infrastruktur.authentication.AuthenticationFilter.REQUEST_USERNAME;
import static no.nav.sikkerhet.authentication.AuthenticationHeaderIdentifier.*;
import static org.apache.commons.lang3.StringUtils.*;

public class SakPEP {
    private static final Logger securitylog = LoggerFactory.getLogger("securitylog");

    static final String RESOURCE_TYPE_SAK = "no.nav.abac.attributter.resource.sak.sak";

    private final ABACClient abacClient;

    private static final Histogram authHistogram = Histogram.build("authorization_request_duration_seconds", "Authorization request duration in seconds")
        .labelNames("consumer", "tokenId", "subjecttype")
        .register();

    private static final Counter authorizationCounter = Counter.build("authorization_result_count", "Authorization result count")
        .labelNames("consumer", "tokenId", "subjecttype", "permission")
        .register();

    public SakPEP(ABACClient abacClient) {
        this.abacClient = abacClient;
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
            abacRequest.addAccessSubject(new ABACAttribute(StandardAttributter.SUBJECT_ID, (String) ctx.getProperty(REQUEST_USERNAME)));
            abacRequest.addAccessSubject(new ABACAttribute(NavAttributter.SUBJECT_FELLES_SUBJECTTYPE, SUBJECT_TYPE_SYSTEMBRUKER.getValue()));
        } else if (Objects.equals(OIDC.getValue(), authIdentifier)) {
            final String tokenBody = substringBetween(token, ".");
            abacRequest.addEnvironment(new ABACAttribute(ENVIRONMENT_FELLES_OIDC_TOKEN_BODY, tokenBody));
        } else if (Objects.equals(SAML.getValue(), authIdentifier)) {
            abacRequest.addEnvironment(new ABACAttribute(ENVIRONMENT_FELLES_SAML_TOKEN, token));
        } else {
            throw new IllegalStateException("Fant ingen gyldig authenticationHeader");
        }

        final Histogram.Timer timer = authHistogram.labels(
            defaultString((String) ctx.getProperty(REQUEST_CONSUMERID), "N/A"),
            defaultString(authIdentifier, "N/A"),
            ContextExtractor.getSubjectType(ctx).getValue()
        ).startTimer();

        final ABACResult abacResult;
        try {
            abacResult = abacClient.execute(abacRequest);
            if (ABACResult.Code.OK.equals(abacResult.getResultCode())) {
                final String arcsightPreparedRequest = stripBrackets(abacRequest.getResources().get(0).getAttributes().toString()); //TODO Dette bør gjøres mer elegant, og robus + test av loggingen.
                String arcsightPreparedResult = StringUtils.remove(stripBrackets(abacResult.toString()), "associatedAdvice=");
                if (abacResult.getAssociatedAdvice().isEmpty()) {
                    arcsightPreparedResult = StringUtils.remove(arcsightPreparedResult, ",");
                }
                if (abacResult.hasAccess()) {
                    securitylog.info("ConsumerID: {}; User: {}; Endpoint: {}; Method: {}; Authorization Request: {}; Authorization Response: {}",
                        ctx.getProperty(REQUEST_CONSUMERID),
                        ctx.getProperty(REQUEST_USERNAME),
                        ctx.getUriInfo().getAbsolutePath(),
                        ctx.getRequest().getMethod(),
                        arcsightPreparedRequest,
                        arcsightPreparedResult);
                } else {
                    securitylog.warn("ConsumerID: {}; User: {}; Endpoint: {}; Method: {}; Authorization Request: {}; Authorization Response: {}",
                        ctx.getProperty(REQUEST_CONSUMERID),
                        ctx.getProperty(REQUEST_USERNAME),
                        ctx.getUriInfo().getAbsolutePath(),
                        ctx.getRequest().getMethod(),
                        arcsightPreparedRequest,
                        arcsightPreparedResult);
                }
                authorizationCounter.labels(
                    defaultString((String) ctx.getProperty(REQUEST_CONSUMERID), "N/A"),
                    defaultString(authIdentifier, "N/A"),
                    ContextExtractor.getSubjectType(ctx).getValue(),
                    abacResult.hasAccess() ? "permit" : "deny").inc();
            }
        } finally {
            timer.observeDuration();
        }
        return abacResult;
    }

    private String stripBrackets(String input) {
        return remove(remove(input, "["), "]");
    }

}
