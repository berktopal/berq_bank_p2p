package p2p_transfer.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** T.C. Kimlik Numarası: 11 hane, ilk hane 0 değil, 10. ve 11. haneler resmi algoritmaya uygun. */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = Tckn.Validator.class)
public @interface Tckn {

    String message() default "Geçerli bir T.C. kimlik numarası giriniz.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<Tckn, String> {
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            return value == null || isValid(value);
        }

        public static boolean isValid(String tckn) {
            if (tckn == null || !tckn.matches("^[1-9]\\d{10}$")) {
                return false;
            }
            int[] d = tckn.chars().map(c -> c - '0').toArray();
            int odd = d[0] + d[2] + d[4] + d[6] + d[8];
            int even = d[1] + d[3] + d[5] + d[7];
            int tenth = Math.floorMod(odd * 7 - even, 10);
            int eleventh = 0;
            for (int i = 0; i < 10; i++) {
                eleventh += d[i];
            }
            return d[9] == tenth && d[10] == eleventh % 10;
        }
    }
}
