package p2p_transfer.dto;

import p2p_transfer.entity.Account;
import p2p_transfer.entity.Transaction;
import p2p_transfer.entity.TransactionStatus;
import p2p_transfer.entity.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * API yanıtlarında dönen veri modelleri.
 * Entity'ler doğrudan dışarı verilmez; böylece şifre, TCKN, e-posta
 * ve başka kullanıcıların bakiyesi gibi hassas alanlar asla sızmaz.
 */
public final class ApiViews {

    private ApiViews() {
    }

    /** Giriş yapan kullanıcının kendi bilgileri. */
    public record UserView(Long id, String firstName, String lastName, String email) {
        public static UserView of(User u) {
            return new UserView(u.getId(), u.getFirstName(), u.getLastName(), u.getEmail());
        }
    }

    /** Kullanıcının kendi hesabı (bakiye sadece hesap sahibine gösterilir). */
    public record AccountView(Long id, String iban, BigDecimal balance, String currency) {
        public static AccountView of(Account a) {
            return new AccountView(a.getId(), a.getIban(), a.getBalance(), a.getCurrency());
        }
    }

    /** IBAN sorgusunda dönen, maskelenmiş alıcı bilgisi. */
    public record AccountLookupView(Long id, String ownerName) {
        public static AccountLookupView of(Account a) {
            User u = a.getUser();
            return new AccountLookupView(a.getId(), u.getFirstName() + " " + mask(u.getLastName()));
        }

        private static String mask(String value) {
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

    public record OwnerView(String firstName, String lastName) {
    }

    /** İşlem geçmişindeki karşı taraf: bakiye veya kişisel veri içermez. */
    public record PartyView(Long id, String iban, OwnerView user) {
        public static PartyView of(Account a) {
            User u = a.getUser();
            return new PartyView(a.getId(), a.getIban(), new OwnerView(u.getFirstName(), u.getLastName()));
        }
    }

    public record TransactionView(Long id, PartyView senderAccount, PartyView receiverAccount,
                                  BigDecimal amount, LocalDateTime transactionDate, TransactionStatus status) {
        public static TransactionView of(Transaction t) {
            return new TransactionView(t.getId(), PartyView.of(t.getSenderAccount()), PartyView.of(t.getReceiverAccount()),
                    t.getAmount(), t.getTransactionDate(), t.getStatus());
        }
    }
}
