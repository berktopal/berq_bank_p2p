package p2p_transfer.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import p2p_transfer.common.Masking;
import p2p_transfer.user.User;

import java.math.BigDecimal;
import java.time.Instant;

public final class PaymentRequestDtos {

    private PaymentRequestDtos() {
    }

    public enum Role { ALL, IN, OUT }

    public enum Direction { INCOMING, OUTGOING }

    public record CreatePaymentRequest(
            @NotNull Long toAccountId,
            @NotBlank String payerIban,
            @NotNull @DecimalMin(value = "0.01", message = "Tutar en az 0,01 olmalıdır.")
            @Digits(integer = 15, fraction = 2, message = "Tutar en fazla 2 ondalık basamak içerebilir.") BigDecimal amount,
            @Size(max = 140) String description) {
    }

    public record PayRequest(@NotNull Long fromAccountId) {
    }

    /**
     * Ödeyen, isteyenin tam adını ve parayı alacak IBAN'ı görür (kime ödediğini bilmeli).
     * İsteyen ise ödeyenin yalnızca maskeli adını görür; onu IBAN ile bulmuştu.
     */
    public record PaymentRequestResponse(Long id, Direction direction, PaymentRequest.Status status, BigDecimal amount,
                                         String currency, String description, String counterpartyName,
                                         String requesterIban, Long requesterAccountId, Long transactionId,
                                         Instant createdAt, Instant expiresAt, Instant respondedAt) {

        public static PaymentRequestResponse of(PaymentRequest r, Long viewerId, Instant now) {
            boolean incoming = r.getPayer().getId().equals(viewerId);
            User other = incoming ? r.getRequester() : r.getPayer();
            String name = incoming ? other.fullName() : Masking.maskedName(other.getFirstName(), other.getLastName());
            return new PaymentRequestResponse(r.getId(), incoming ? Direction.INCOMING : Direction.OUTGOING,
                    r.effectiveStatus(now), r.getAmount(), r.getCurrency(), r.getDescription(), name,
                    r.getRequesterAccount().getIban(), incoming ? null : r.getRequesterAccount().getId(),
                    r.getTransaction() == null ? null : r.getTransaction().getId(),
                    r.getCreatedAt(), r.getExpiresAt(), r.getRespondedAt());
        }
    }
}
