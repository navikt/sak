package no.nav.sak.validering;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;


public class OrganisasjonsnummerValidator implements ConstraintValidator<Organisasjonsnummer, String> {

    @Override
    public void initialize(Organisasjonsnummer orgnr) {
        //sonar
    }

    @Override
    public boolean isValid(String orgnr, ConstraintValidatorContext constraintValidatorContext) {
        return isValid(orgnr);
    }

    public static boolean isValid(String orgnr) {
        if (StringUtils.isEmpty(orgnr)) {
            return true;
        } else if(orgnr.matches("^[0-9]{9}$")) {
            return true;
        } else {
            return false;
        }
    }

}
