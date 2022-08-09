package no.nav.sak.validering;


import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import static no.nav.sak.validering.CountFieldsMatching.count;

public class AllorNoneOfValidator implements ConstraintValidator<AllOrNoneOf, Object> {
    private String[] fields;

    @Override
    public void initialize(AllOrNoneOf allOrNoneOf) {
        this.fields = allOrNoneOf.fields();
    }

    @Override
    public boolean isValid(Object o, ConstraintValidatorContext constraintValidatorContext) {
        Long countWithValue = count(o, fields);
        return countWithValue == 0 || countWithValue == fields.length;
    }
}
