package no.nav.sak.infrastruktur.abac;

import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
import no.nav.abac.xacml.NavAttributter;
import no.nav.abac.xacml.StandardAttributter;
import no.nav.sak.SakConfiguration;
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
    private static final Logger log = LoggerFactory.getLogger("securitylog");

    static final String RESOURCE_TYPE_SAK = "no.nav.abac.attributter.resource.sak.sak";

    private final ABACClient abacClient;
    private final SakConfiguration sakConfiguration;

    private static final Histogram authHistogram = Histogram.build("authorization_request_duration_seconds", "Authorization request duration in seconds")
        .labelNames("consumer", "tokenId", "subjecttype")
        .register();

    private static final Counter authorizationCounter = Counter.build("authorization_result_count", "Authorization result count")
        .labelNames("permission")
        .register();

    public SakPEP(ABACClient abacClient, SakConfiguration sakConfiguration) {
        this.abacClient = abacClient;
        this.sakConfiguration = sakConfiguration;
    }

    public ABACResult autoriser(ContainerRequestContext ctx, AuthorizationRequest authorizationRequest) {
        if(!performAuthorization(ctx)) {
            log.info("ConsumerID: {}; User: {}; Endpoint: {}; Method: {}; Authorization disabled for {}",
                ctx.getProperty(REQUEST_CONSUMERID),
                ctx.getProperty(REQUEST_USERNAME),
                ctx.getUriInfo().getAbsolutePath(),
                ctx.getRequest().getMethod(),
                !sakConfiguration.getBoolean("ABAC_ENABLED", true) ? "ALL" : "SERVICE USERS");
            return new ABACResult(ABACDecision.PERMIT.getValue(), null);
        }

        ABACRequest abacRequest = ABACRequest.newRequest()
            .addEnvironment(new ABACAttribute(ENVIRONMENT_FELLES_PEP_ID ,"sak"))
            .addResource(new ABACAttribute(RESOURCE_FELLES_DOMENE, "sak"))
            .addResource(new ABACAttribute(RESOURCE_FELLES_RESOURCE_TYPE, RESOURCE_TYPE_SAK));

        authorizationRequest.getAktoerId().ifPresent(aktoerId -> abacRequest.addResource(new ABACAttribute(RESOURCE_FELLES_PERSON_AKTOERID_RESOURCE, aktoerId)));
        authorizationRequest.getTema().ifPresent(tema -> abacRequest.addResource(new ABACAttribute(RESOURCE_FELLES_TEMA, tema)));

        String authIdentifier = substringBefore(trim(ctx.getHeaderString(AUTHORIZATION)), " ");
        String token = substringAfter(trim(ctx.getHeaderString(AUTHORIZATION)), " ");

        if(Objects.equals(BASIC.getValue(), authIdentifier)) {
            abacRequest.addAccessSubject(new ABACAttribute(StandardAttributter.SUBJECT_ID, (String)ctx.getProperty(REQUEST_USERNAME)));
            abacRequest.addAccessSubject(new ABACAttribute(NavAttributter.SUBJECT_FELLES_SUBJECTTYPE, SUBJECT_TYPE_SYSTEMBRUKER.getValue()));
        } else if(Objects.equals(OIDC.getValue(), authIdentifier)) {
            String tokenBody = substringBetween(token, ".");
            abacRequest.addEnvironment(new ABACAttribute(ENVIRONMENT_FELLES_OIDC_TOKEN_BODY, tokenBody));
        } else if(Objects.equals(SAML.getValue(), authIdentifier)) {
            abacRequest.addEnvironment(new ABACAttribute(ENVIRONMENT_FELLES_SAML_TOKEN, token));
        } else {
            throw new IllegalStateException("Fant ingen gyldig authenticationHeader");
        }


        Histogram.Timer timer = authHistogram.labels(
            defaultString((String)ctx.getProperty(REQUEST_CONSUMERID), "N/A"),
            defaultString(authIdentifier, "N/A"),
            ContextExtractor.getSubjectType(ctx).getValue()
        ).startTimer();

        ABACResult abacResult;
        try {
            abacResult = abacClient.execute(abacRequest);
            String arcsightPreparedRequest = stripBrackets(abacRequest.getResource().getAttributes().toString());
            String arcsightPreparedResult = StringUtils.remove(stripBrackets(abacResult.toString()), "associatedAdvice=");
            if(abacResult.getAssociatedAdvice().isEmpty()) {
                arcsightPreparedResult = StringUtils.remove(arcsightPreparedResult, ",");
            }
            if(abacResult.hasAccess()) {
                log.info("ConsumerID: {}; User: {}; Endpoint: {}; Method: {}; Authorization Request: {}; Authorization Response: {}",
                    ctx.getProperty(REQUEST_CONSUMERID),
                    ctx.getProperty(REQUEST_USERNAME),
                    ctx.getUriInfo().getAbsolutePath(),
                    ctx.getRequest().getMethod(),
                    arcsightPreparedRequest,
                    arcsightPreparedResult);
            } else {
                log.warn("ConsumerID: {}; User: {}; Endpoint: {}; Method: {}; Authorization Request: {}; Authorization Response: {}",
                    ctx.getProperty(REQUEST_CONSUMERID),
                    ctx.getProperty(REQUEST_USERNAME),
                    ctx.getUriInfo().getAbsolutePath(),
                    ctx.getRequest().getMethod(),
                    arcsightPreparedRequest,
                    arcsightPreparedResult);
            }
            authorizationCounter.labels(abacResult.hasAccess() ? "permit" : "deny").inc();
        } finally {
            timer.observeDuration();
        }
       return abacResult;
    }

    private String stripBrackets(String input) {
        return remove(remove(input, "["), "]");
    }

    private boolean performAuthorization(ContainerRequestContext ctx) {
        boolean abacEnabled = sakConfiguration.getBoolean("ABAC_ENABLED", true);
        boolean abacEnabledServiceUsers = sakConfiguration.getBoolean("ABAC_ENABLED_SERVICEUSERS", true);
        boolean serviceUser = Objects.equals(SUBJECT_TYPE_SYSTEMBRUKER, ContextExtractor.getSubjectType(ctx));
        return abacEnabled && (abacEnabledServiceUsers  || !serviceUser);
    }
}
