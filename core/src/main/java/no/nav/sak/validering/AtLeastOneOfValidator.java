package no.nav.sak.validering;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import static org.apache.commons.lang3.math.NumberUtils.LONG_ONE;

public class AtLeastOneOfValidator implements ConstraintValidator<AtLeastOneOf, Object> {
    private String[] fields;

    @Override
    public void initialize(AtLeastOneOf atLeastOneOf) {
        this.fields = atLeastOneOf.fields();
    }

    @Override
    public boolean isValid(Object o, ConstraintValidatorContext constraintValidatorContext) {
        return CountFieldsMatching.count(o, fields) >= LONG_ONE;
    }
}
