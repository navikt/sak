package no.nav.sak.infrastruktur.abac;

import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
import no.nav.abac.xacml.NavAttributter;
import no.nav.abac.xacml.StandardAttributter;
import no.nav.sak.Sak;
import no.nav.sikkerhet.abac.ABACAttribute;
import no.nav.sikkerhet.abac.ABACClient;
import no.nav.sikkerhet.abac.ABACRequest;
import no.nav.sikkerhet.abac.ABACResult;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.ContainerRequestContext;
import java.util.Objects;

import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static no.nav.abac.xacml.NavAttributter.*;
import static no.nav.sak.infrastruktur.authentication.AuthenticationFilter.REQUEST_CONSUMERID;
import static no.nav.sak.infrastruktur.authentication.AuthenticationFilter.REQUEST_USERNAME;
import static no.nav.sikkerhet.authentication.AuthenticationHeaderIdentifier.*;
import static org.apache.commons.lang3.StringUtils.*;

public class SakPEP {
    private static final Logger log = LoggerFactory.getLogger("securitylog");
    private static final String RESOURCE_TYPE_SAK = "no.nav.abac.attributter.resource.sak.sak";
    private static final String SUBJECT_TYPE_SYSTEMBRUKER="systemressurs";

    private final ABACClient abacClient;
    private static final Histogram authHistogram = Histogram.build("authorization_request_duration_seconds", "Authorization request duration in seconds")
        .labelNames("consumer", "tokenId")
        .register();
    private static final Counter authorizationCounter = Counter.build("authorization_result_count", "Authorization result count")
        .labelNames("permission")
        .register();
    public SakPEP(ABACClient abacClient) {
        this.abacClient = abacClient;
    }

    public ABACResult autoriser(ContainerRequestContext ctx, Sak sak) {
        ABACRequest abacRequest = ABACRequest.newRequest()
            .addEnvironment(new ABACAttribute(ENVIRONMENT_FELLES_PEP_ID ,"sak"))
            .addResource(new ABACAttribute(RESOURCE_FELLES_DOMENE, "sak"))
            .addResource(new ABACAttribute(RESOURCE_FELLES_RESOURCE_TYPE, RESOURCE_TYPE_SAK))
            .addResource(new ABACAttribute(RESOURCE_FELLES_TEMA, sak.getTema()));

        if(StringUtils.isNotBlank(sak.getAktoerId())) {
            abacRequest.addResource(new ABACAttribute(RESOURCE_FELLES_PERSON_AKTOERID_RESOURCE, sak.getAktoerId()));
        }

        String authIdentifier = substringBefore(trim(ctx.getHeaderString(AUTHORIZATION)), " ");
        String token = substringAfter(trim(ctx.getHeaderString(AUTHORIZATION)), " ");
        if(Objects.equals(BASIC.getValue(), authIdentifier)) {
            abacRequest.addAccessSubject(new ABACAttribute(StandardAttributter.SUBJECT_ID, (String)ctx.getProperty(REQUEST_USERNAME)));
            abacRequest.addAccessSubject(new ABACAttribute(NavAttributter.SUBJECT_FELLES_SUBJECTTYPE, SUBJECT_TYPE_SYSTEMBRUKER));
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
            defaultString(authIdentifier, "N/A")).startTimer();
        ABACResult abacResult;
        try {
            abacResult = abacClient.execute(abacRequest);
            log.info("Autorisering for bruker: \"{}\" mot endepunkt: \"{}\", metode: \"{}\". Autorisasjonsforespørsel: {}. Autorisasjonsresultat: {}",
                ctx.getProperty(REQUEST_USERNAME),
                ctx.getUriInfo().getAbsolutePath(),
                ctx.getRequest().getMethod(),
                abacRequest.getResource().getAttributes(),
                abacResult);
            authorizationCounter.labels(abacResult.hasAccess() ? "permit" : "deny").inc();
        } finally {
            timer.observeDuration();
        }
       return abacResult;
    }
}
