package p2p_transfer.common;

/** Kişisel verinin yalnızca gerekli kısmını gösteren yardımcılar (KVKK veri minimizasyonu). */
public final class Masking {

    private Masking() {
    }

    /** "Berk Topal" → "Berk T****": alıcı doğrulamaya yeter, soyadını ifşa etmez. */
    public static String maskedName(String firstName, String lastName) {
        return firstName + " " + maskWords(lastName);
    }

    /** "12345678901" → "123******01". */
    public static String maskTckn(String tckn) {
        if (tckn == null || tckn.length() < 6) {
            return "***";
        }
        return tckn.substring(0, 3) + "*".repeat(tckn.length() - 5) + tckn.substring(tckn.length() - 2);
    }

    private static String maskWords(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String part : value.trim().split("\\s+")) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(part.charAt(0)).append("*".repeat(Math.max(part.length() - 1, 1)));
        }
        return sb.toString();
    }
}
