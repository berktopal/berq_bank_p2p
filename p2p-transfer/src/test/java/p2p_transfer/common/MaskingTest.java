package p2p_transfer.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MaskingTest {

    @Test
    void masksEveryWordOfTheSurname() {
        assertThat(Masking.maskedName("Berk", "Topal")).isEqualTo("Berk T****");
        assertThat(Masking.maskedName("Ali", "Can Öz")).isEqualTo("Ali C** Ö*");
    }

    @Test
    void singleLetterSurnameStillHidesSomething() {
        assertThat(Masking.maskedName("Ece", "K")).isEqualTo("Ece K*");
    }

    @Test
    void tcknKeepsOnlyFirstThreeAndLastTwoDigits() {
        assertThat(Masking.maskTckn("10000000146")).isEqualTo("100******46");
    }
}
