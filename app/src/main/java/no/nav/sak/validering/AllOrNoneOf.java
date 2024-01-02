package no.nav.sak.validering;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = AllorNoneOfValidator.class)
public @interface AllOrNoneOf {
    String message() default "{no.nav.sak.AllOrNoneOf}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    String[] fields();

}
