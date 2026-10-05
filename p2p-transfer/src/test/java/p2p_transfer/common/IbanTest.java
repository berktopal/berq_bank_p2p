package p2p_transfer.common;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IbanTest {

    @Test
    void acceptsRealWorldTurkishIban() {
        assertThat(Iban.hasValidChecksum("TR330006100519786457841326")).isTrue();
    }

    @Test
    void rejectsIbanWithWrongCheckDigits() {
        assertThat(Iban.hasValidChecksum("TR340006100519786457841326")).isFalse();
    }

    @RepeatedTest(50)
    void generatedIbansAreWellFormedAndPassMod97() {
        String iban = Iban.generate("00999");
        assertThat(iban).hasSize(26).startsWith("TR").matches("TR\\d{2}009990\\d{16}");
        assertThat(Iban.hasValidChecksum(iban)).isTrue();
    }

    @Test
    void normalizeStripsWhitespaceAndUppercases() {
        assertThat(Iban.normalize(" tr33 0006 1005 1978 6457 8413 26 ")).isEqualTo("TR330006100519786457841326");
        assertThat(Iban.normalize(null)).isEmpty();
    }

    @Test
    void formatGroupsByFour() {
        assertThat(Iban.format("TR330006100519786457841326")).isEqualTo("TR33 0006 1005 1978 6457 8413 26");
    }

    @Test
    void formatCheckRejectsForeignOrShortValues() {
        assertThat(Iban.isTrFormat("DE89370400440532013000")).isFalse();
        assertThat(Iban.isTrFormat("TR12345")).isFalse();
    }

    @Test
    void bankCodeMustBeFiveDigits() {
        assertThatThrownBy(() -> Iban.generate("12")).isInstanceOf(IllegalArgumentException.class);
    }
}
