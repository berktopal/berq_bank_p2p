package p2p_transfer.common;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import p2p_transfer.common.validation.Tckn;
import p2p_transfer.demo.DemoDataSeeder;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class TcknTest {

    @ParameterizedTest
    @ValueSource(strings = {"10000000146", "11111111110"})
    void acceptsAlgorithmicallyValidNumbers(String tckn) {
        assertThat(Tckn.Validator.isValid(tckn)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345678901", "01234567890", "1000000014", "1000000014a", "10000000147", ""})
    void rejectsInvalidNumbers(String tckn) {
        assertThat(Tckn.Validator.isValid(tckn)).isFalse();
    }

    @RepeatedTest(20)
    void demoGeneratorProducesValidNumbers() {
        assertThat(Tckn.Validator.isValid(DemoDataSeeder.randomTckn(new Random()))).isTrue();
    }
}
