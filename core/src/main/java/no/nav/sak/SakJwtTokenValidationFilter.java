package no.nav.sak;

import lombok.extern.slf4j.Slf4j;
import no.nav.security.token.support.core.configuration.MultiIssuerConfiguration;
import no.nav.security.token.support.core.validation.JwtTokenRetriever;
import no.nav.security.token.support.core.validation.JwtTokenValidationHandler;
import no.nav.security.token.support.filter.JwtTokenValidationFilter;
import no.nav.security.token.support.jaxrs.JaxrsTokenValidationContextHolder;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
public class SakJwtTokenValidationFilter extends JwtTokenValidationFilter {

    public SakJwtTokenValidationFilter(MultiIssuerConfiguration oidcConfig) {
        super(new JwtTokenValidationHandler(oidcConfig), JaxrsTokenValidationContextHolder.getHolder());
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (request instanceof HttpServletRequest) {

            String authorizationType = ( (HttpServletRequest) request).getHeader("Authorization");
            log.info("passing through JWT validation filter with authorization type " + authorizationType);
            if ("Bearer".equals(authorizationType)) {
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
