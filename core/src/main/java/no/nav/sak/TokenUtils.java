package no.nav.sak;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import no.nav.security.token.support.core.context.TokenValidationContext;
import no.nav.security.token.support.core.context.TokenValidationContextHolder;
import no.nav.security.token.support.core.jwt.JwtToken;
import no.nav.security.token.support.core.jwt.JwtTokenClaims;
import no.nav.security.token.support.jaxrs.JaxrsTokenValidationContextHolder;

public class TokenUtils {
    
        private static TokenValidationContextHolder contextHolder = JaxrsTokenValidationContextHolder.getHolder();

        public static final String ISSUER_AZUREAD = "azuread";
        public static final String ISSUER_ISSO = "isso";
        public static final String ISSUER_RESTSTS = "reststs";
        public static final String ISSUER_TOKENX ="azuread";
        public static final String FSS_PROXY_AUTHORIZATION_HEADER = "x-fss-proxy-authorization";
        

    
     
        
       
        public static boolean hasTokenForIssuer(String issuer) {
           
           return contextHolder.getTokenValidationContext() != null ? contextHolder.getTokenValidationContext().hasTokenFor(issuer) : false; 
        }
        
        private static String getSubject(JwtToken token) {
             JwtTokenClaims claims = token.getJwtTokenClaims();
            
             Optional<String> fnr = Optional.of( claims.getStringClaim("pid") != null ? claims.getStringClaim("pid") : claims.getStringClaim("sub"));
             
             return fnr.orElseThrow(() -> new RuntimeException("Missing user claim"));
        }
        
        public static String getSubject() {
            
            TokenValidationContext context = contextHolder.getTokenValidationContext();
            if (context==null) {
                return null;
            }
            else if (context.hasTokenFor(ISSUER_TOKENX)) {
                return getSubject( context.getJwtToken(ISSUER_TOKENX) );
            }
            else if ( context.hasTokenFor(ISSUER_ISSO) ) {
                return getSubject(context.getJwtToken(ISSUER_ISSO));
            }
            else {
                return null;
            }
        }
        
        public static String getTokenAsString(String issuer) {
            TokenValidationContext context = contextHolder.getTokenValidationContext();
            
            switch (issuer) {
              
                case ISSUER_ISSO:
                case  ISSUER_TOKENX : {
                                        if (!context.hasTokenFor(issuer)) {
                                            throw  new RuntimeException("No valid token for issuer: " + issuer );
                                        }
                                        return context.getJwtToken(issuer).getTokenAsString() ;
                                      }
            
             default: throw new RuntimeException("Unknown issuer:" + issuer);
            }
            
        }

}
