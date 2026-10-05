package p2p_transfer.notification;

import p2p_transfer.transfer.TransactionCategory;

import java.math.BigDecimal;
import java.time.Instant;

public record NotificationResponse(Long id, NotificationType type, BigDecimal amount, String currency,
                                   String counterpartyName, TransactionCategory category, Long transactionId,
                                   Long paymentRequestId, Long scheduledTransferId, String errorCode,
                                   boolean read, Instant createdAt) {

    public static NotificationResponse of(Notification n) {
        return new NotificationResponse(n.getId(), n.getType(), n.getAmount(), n.getCurrency(), n.getCounterpartyName(),
                n.getCategory(), n.getTransactionId(), n.getPaymentRequestId(), n.getScheduledTransferId(),
                n.getErrorCode(), n.getReadAt() != null, n.getCreatedAt());
    }
}
