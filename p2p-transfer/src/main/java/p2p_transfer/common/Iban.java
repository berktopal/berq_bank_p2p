package p2p_transfer.common;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.regex.Pattern;

/**
 * Türkiye IBAN yardımcıları.
 * Yapı: TR + 2 kontrol hanesi + 5 hane banka kodu + 1 hane rezerv (0) + 16 hane hesap no = 26 karakter.
 */
public final class Iban {

    private static final Pattern TR_FORMAT = Pattern.compile("^TR\\d{24}$");
    private static final BigInteger NINETY_SEVEN = BigInteger.valueOf(97);
    private static final SecureRandom RANDOM = new SecureRandom();

    private Iban() {
    }

    /** Boşlukları siler ve büyük harfe çevirir; {@code null} için boş string döner. */
    public static String normalize(String raw) {
        return raw == null ? "" : raw.replaceAll("\\s+", "").toUpperCase();
    }

    /** Biçim kontrolü. Mevcut (eski) kayıtlarla uyum için checksum burada zorunlu tutulmaz. */
    public static boolean isTrFormat(String iban) {
        return iban != null && TR_FORMAT.matcher(iban).matches();
    }

    /** ISO 13616 mod-97 doğrulaması. */
    public static boolean hasValidChecksum(String iban) {
        if (!isTrFormat(iban)) {
            return false;
        }
        String rearranged = iban.substring(4) + iban.substring(0, 4);
        return toNumeric(rearranged).mod(NINETY_SEVEN).intValue() == 1;
    }

    /** Verilen banka kodu için kontrol haneleri doğru hesaplanmış rastgele bir TR IBAN üretir. */
    public static String generate(String bankCode) {
        if (bankCode == null || !bankCode.matches("\\d{5}")) {
            throw new IllegalArgumentException("Bank code must be 5 digits");
        }
        StringBuilder accountNo = new StringBuilder(16);
        for (int i = 0; i < 16; i++) {
            accountNo.append(RANDOM.nextInt(10));
        }
        String bban = bankCode + "0" + accountNo;
        int check = 98 - toNumeric(bban + "TR00").mod(NINETY_SEVEN).intValue();
        return "TR" + String.format("%02d", check) + bban;
    }

    /** 1234 5678 ... şeklinde gruplar (görüntüleme için). */
    public static String format(String iban) {
        return normalize(iban).replaceAll("(.{4})(?!$)", "$1 ");
    }

    private static BigInteger toNumeric(String value) {
        StringBuilder digits = new StringBuilder(value.length() * 2);
        for (char c : value.toCharArray()) {
            digits.append(Character.isLetter(c) ? String.valueOf(c - 'A' + 10) : String.valueOf(c));
        }
        return new BigInteger(digits.toString());
    }
}
