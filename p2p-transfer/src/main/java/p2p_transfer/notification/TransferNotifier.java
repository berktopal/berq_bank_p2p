package p2p_transfer.notification;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import p2p_transfer.transfer.Transaction;
import p2p_transfer.transfer.TransferCompleted;

/** Gelen her transferde alıcıya "para geldi" bildirimi (kendi hesapları arası virmanlar ve istek ödemeleri hariç). */
@Component
public class TransferNotifier {

    private final NotificationService notifications;

    public TransferNotifier(NotificationService notifications) {
        this.notifications = notifications;
    }

    @EventListener
    public void on(TransferCompleted event) {
        // İstek ödendiğinde isteyene daha açıklayıcı PAYMENT_REQUEST_PAID gider; ikisi birden gönderilmez
        if (event.internal() || event.origin() == TransferCompleted.Origin.PAYMENT_REQUEST) {
            return;
        }
        Transaction tx = event.transaction();
        notifications.notify(tx.getReceiverAccount().getUser().getId(), NotificationType.MONEY_RECEIVED, n -> {
            n.setAmount(tx.getAmount());
            n.setCurrency(tx.getCurrency());
            n.setCounterpartyName(tx.getSenderAccount().getUser().fullName());
            n.setCategory(tx.getCategory());
            n.setTransactionId(tx.getId());
        });
    }
}
