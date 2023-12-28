package no.nav.sak.infrastruktur;

import no.nav.sak.infrastruktur.authentication.AuthenticationFilter;
import org.apache.commons.lang3.StringUtils;

import jakarta.ws.rs.container.ContainerRequestContext;

import static no.nav.sak.infrastruktur.SubjectType.SUBJECT_TYPE_EKSTERNBRUKER;
import static no.nav.sak.infrastruktur.SubjectType.SUBJECT_TYPE_INTERNBRUKER;
import static no.nav.sak.infrastruktur.SubjectType.SUBJECT_TYPE_SYSTEMBRUKER;

public class ContextExtractor {

    private ContextExtractor() {
        //Util
    }

    public static String getConsumerID(ContainerRequestContext ctx) {
        if(ctx.getProperty(AuthenticationFilter.REQUEST_CONSUMERID) == null) {
            return "N/A";
        }
        return (String)ctx.getProperty(AuthenticationFilter.REQUEST_CONSUMERID);
    }

    public static String getUserName(ContainerRequestContext ctx) {
        return (String) ctx.getProperty(AuthenticationFilter.REQUEST_USERNAME);
    }

    public static SubjectType getSubjectType(ContainerRequestContext ctx) {
        String username = getUserName(ctx);
        if (StringUtils.startsWith(username, "srv")) {
            return SUBJECT_TYPE_SYSTEMBRUKER;
        } else if (StringUtils.isNumeric(username) && StringUtils.length(username) == 11) {
            return SUBJECT_TYPE_EKSTERNBRUKER;
        } else {
            return SUBJECT_TYPE_INTERNBRUKER;
        }
    }
}
