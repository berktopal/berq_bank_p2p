package p2p_transfer.transfer;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import p2p_transfer.account.Account;
import p2p_transfer.user.User;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public final class TransferDtos {

    private TransferDtos() {
    }

    public record TransferRequest(
            @NotNull Long fromAccountId,
            @NotBlank String toIban,
            @NotNull @DecimalMin(value = "0.01", message = "Tutar en az 0,01 olmalıdır.")
            @Digits(integer = 15, fraction = 2, message = "Tutar en fazla 2 ondalık basamak içerebilir.") BigDecimal amount,
            @Size(max = 140) String description,
            TransactionCategory category) {
    }

    public enum Direction { INCOMING, OUTGOING }

    public record TransactionFilter(Long accountId, Direction direction, LocalDate from, LocalDate to,
                                    TransactionCategory category, String q) {
    }

    public record AccountRef(Long id, String iban, String name) {
    }

    public record Counterparty(String name, String iban) {
    }

    /**
     * Bir işlemin, onu görüntüleyen kullanıcı açısından görünümü.
     * {@code internal}: iki taraf da kullanıcının kendi hesabı.
     */
    public record TransactionResponse(Long id, String reference, Direction direction, boolean internal,
                                      BigDecimal amount, String currency, String description,
                                      TransactionCategory category, TransactionStatus status, Instant createdAt,
                                      AccountRef account, Counterparty counterparty, BigDecimal balanceAfter) {

        /**
         * @param perspectiveAccountId bakış açısının hesabı; {@code null} ise kullanıcının taraf olduğu ilk hesap
         */
        public static TransactionResponse of(Transaction t, Long userId, Long perspectiveAccountId) {
            Account sender = t.getSenderAccount();
            Account receiver = t.getReceiverAccount();
            boolean senderMine = sender.isOwnedBy(userId);
            boolean receiverMine = receiver.isOwnedBy(userId);

            boolean outgoing = perspectiveAccountId != null
                    ? sender.getId().equals(perspectiveAccountId)
                    : senderMine;

            Account mine = outgoing ? sender : receiver;
            Account other = outgoing ? receiver : sender;
            User otherUser = other.getUser();
            String otherName = senderMine && receiverMine ? other.getName() : otherUser.fullName();

            return new TransactionResponse(t.getId(), t.getReference(),
                    outgoing ? Direction.OUTGOING : Direction.INCOMING, senderMine && receiverMine,
                    t.getAmount(), t.getCurrency(), t.getDescription(), t.getCategory(), t.getStatus(), t.getCreatedAt(),
                    new AccountRef(mine.getId(), mine.getIban(), mine.getName()),
                    new Counterparty(otherName, other.getIban()),
                    outgoing ? t.getSenderBalanceAfter() : t.getReceiverBalanceAfter());
        }
    }
}
