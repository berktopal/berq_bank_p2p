package p2p_transfer.transfer;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/** "BQ" + 14 karakter Crockford Base32 (karışabilecek I, L, O, U harfleri yok) → ör. BQ7K2M9X4TQ1HZ8C. */
@Component
public class ReferenceGenerator {

    private static final char[] ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    public String next() {
        StringBuilder sb = new StringBuilder("BQ");
        for (int i = 0; i < 14; i++) {
            sb.append(ALPHABET[RANDOM.nextInt(ALPHABET.length)]);
        }
        return sb.toString();
    }
}
