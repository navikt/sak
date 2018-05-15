package no.nav.sak.infrastruktur.abac;

import io.prometheus.client.Histogram;
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
    private final ABACClient abacClient;
    private static final Histogram authHistogram = Histogram.build("authorization_request_duration_seconds", "Authorization request duration in seconds")
        .labelNames("consumer", "tokenId")
        .register();
    public SakPEP(ABACClient abacClient) {
        this.abacClient = abacClient;
    }

    public ABACResult autoriser(ContainerRequestContext ctx, Sak sak) {
        ABACRequest abacRequest = ABACRequest.newRequest()
              .addResource(new ABACAttribute(RESOURCE_FELLES_DOMENE, "sak"))
            .addResource(new ABACAttribute(RESOURCE_FELLES_RESOURCE_TYPE, RESOURCE_TYPE_SAK))
            .addResource(new ABACAttribute(RESOURCE_FELLES_TEMA, sak.getTema()));

        if(StringUtils.isNotBlank(sak.getAktoerId())) {
            abacRequest.addResource(new ABACAttribute(RESOURCE_FELLES_PERSON_AKTOERID_RESOURCE, sak.getAktoerId()));
        }

        String authIdentifier = substringBefore(trim(ctx.getHeaderString(AUTHORIZATION)), " ");
        String token = substringAfter(trim(ctx.getHeaderString(AUTHORIZATION)), " ");
        if(Objects.equals(BASIC.getValue(), authIdentifier)) {
            abacRequest.addAccessSubject(new ABACAttribute(StandardAttributter.SUBJECT_ID, ctx.getHeaderString(REQUEST_USERNAME)));
        } else if(Objects.equals(OIDC.getValue(), authIdentifier)) {
            String tokenBody = substringBetween(token, ".");
            abacRequest.addEnvironment(new ABACAttribute(ENVIRONMENT_FELLES_OIDC_TOKEN_BODY, tokenBody));
        } else if(Objects.equals(SAML.getValue(), authIdentifier)) {
            abacRequest.addEnvironment(new ABACAttribute(ENVIRONMENT_FELLES_SAML_TOKEN, token));
        } else {
            throw new IllegalStateException("Fant ingen gyldig authenticationHeader");
        }
        Histogram.Timer timer = authHistogram.labels(ctx.getHeaderString(REQUEST_CONSUMERID), authIdentifier).startTimer();
        ABACResult abacResult;
        try {
            abacResult = abacClient.execute(abacRequest);
            log.info("Autorisering for bruker: {} til endepunkt: {}, metode: {}. Autorisasjonsforespørsel: {}. Autorisasjonsresultat: {}",
                ctx.getProperty(REQUEST_USERNAME),
                ctx.getUriInfo().getAbsolutePath(),
                ctx.getRequest().getMethod(),
                abacRequest,
                abacResult);
        } finally {
            timer.observeDuration();
        }
       return abacResult;
    }
}
