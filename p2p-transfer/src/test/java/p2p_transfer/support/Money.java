package p2p_transfer.support;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.math.BigDecimal;

/** JSON'daki sayıyı (0, 900.5, 900.50...) ölçekten bağımsız olarak BigDecimal gibi karşılaştırır. */
public final class Money {

    private Money() {
    }

    public static Matcher<Object> eq(String expected) {
        BigDecimal exp = new BigDecimal(expected);
        return new TypeSafeMatcher<>() {
            @Override
            protected boolean matchesSafely(Object actual) {
                return actual instanceof Number && new BigDecimal(actual.toString()).compareTo(exp) == 0;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("amount equal to ").appendValue(exp);
            }
        };
    }
}
