package no.nav.sak.infrastruktur;

import jakarta.servlet.http.HttpServletRequest;
import no.nav.sak.infrastruktur.authentication.AuthenticationFilter;
import org.apache.commons.lang3.StringUtils;

import static no.nav.sak.infrastruktur.SubjectType.SUBJECT_TYPE_EKSTERNBRUKER;
import static no.nav.sak.infrastruktur.SubjectType.SUBJECT_TYPE_INTERNBRUKER;
import static no.nav.sak.infrastruktur.SubjectType.SUBJECT_TYPE_SYSTEMBRUKER;

public class ContextExtractor {

    private ContextExtractor() {
        //Util
    }

	public static String getUserName(HttpServletRequest httpServletRequest) {
        return (String) httpServletRequest.getAttribute(AuthenticationFilter.REQUEST_USERNAME);
    }

	public static SubjectType getSubjectType(HttpServletRequest httpServletRequest) {
		String username = getUserName(httpServletRequest);
		if (StringUtils.startsWith(username, "srv")) {
			return SUBJECT_TYPE_SYSTEMBRUKER;
		} else if (StringUtils.isNumeric(username) && StringUtils.length(username) == 11) {
			return SUBJECT_TYPE_EKSTERNBRUKER;
		} else {
			return SUBJECT_TYPE_INTERNBRUKER;
		}
	}
}
