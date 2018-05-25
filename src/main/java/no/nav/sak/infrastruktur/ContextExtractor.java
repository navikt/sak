package no.nav.sak.infrastruktur;

import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.container.ContainerRequestContext;

import static no.nav.sak.infrastruktur.SubjectType.SUBJECT_TYPE_EKSTERNBRUKER;
import static no.nav.sak.infrastruktur.SubjectType.SUBJECT_TYPE_INTERNBRUKER;
import static no.nav.sak.infrastruktur.SubjectType.SUBJECT_TYPE_SYSTEMBRUKER;
import static no.nav.sak.infrastruktur.authentication.AuthenticationFilter.REQUEST_CONSUMERID;
import static no.nav.sak.infrastruktur.authentication.AuthenticationFilter.REQUEST_USERNAME;

public class ContextExtractor {

    private ContextExtractor() {
        //Util
    }

    public static String getConsumerID(ContainerRequestContext ctx) {
        if(ctx.getProperty(REQUEST_CONSUMERID) == null) {
            return "N/A";
        }
        return (String)ctx.getProperty(REQUEST_CONSUMERID);
    }

    public static SubjectType getSubjectType(ContainerRequestContext ctx) {
        String username = (String) ctx.getProperty(REQUEST_USERNAME);
        if (StringUtils.startsWith(username, "srv")) {
            return SUBJECT_TYPE_SYSTEMBRUKER;
        } else if (StringUtils.isNumeric(username) && StringUtils.length(username) == 11) {
            return SUBJECT_TYPE_EKSTERNBRUKER;
        } else {
            return SUBJECT_TYPE_INTERNBRUKER;
        }
    }
}
