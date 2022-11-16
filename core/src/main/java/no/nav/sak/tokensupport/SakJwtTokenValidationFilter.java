package no.nav.sak.tokensupport;

import lombok.extern.slf4j.Slf4j;
import no.nav.security.token.support.core.configuration.MultiIssuerConfiguration;
import no.nav.security.token.support.core.validation.JwtTokenRetriever;
import no.nav.security.token.support.core.validation.JwtTokenValidationHandler;
import no.nav.security.token.support.filter.JwtTokenValidationFilter;
import no.nav.security.token.support.jaxrs.JaxrsTokenValidationContextHolder;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;


import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.apache.commons.lang3.StringUtils.trim;

@Slf4j
public class SakJwtTokenValidationFilter extends JwtTokenValidationFilter {

    private static String AUTORIZATION_BEARER = "Bearer";

    public SakJwtTokenValidationFilter(MultiIssuerConfiguration oidcConfig) {
        super(new JwtTokenValidationHandler(oidcConfig), JaxrsTokenValidationContextHolder.getHolder());
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (request instanceof HttpServletRequest) {
            String authorizationHeader = ( (HttpServletRequest) request).getHeader(HttpHeaders.AUTHORIZATION);
            String authorizationType = StringUtils.substringBefore(trim(authorizationHeader), " ");

            if (AUTORIZATION_BEARER.equals(authorizationType)) {
                super.doFilter(request,response,chain);
            }
            else {
                chain.doFilter(request,response);
            }
        } else {
            chain.doFilter(request, response);
        }
    }
}
