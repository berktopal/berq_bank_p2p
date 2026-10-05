package p2p_transfer.transfer;

/**
 * Bir transfer veritabanına yazıldı (henüz commit edilmedi). Dinleyiciler aynı transaction içinde çalışır:
 * bildirim veya bütçe uyarısı yazılamazsa transfer de geri alınır, transfer geri alınırsa onlar da yazılmaz.
 */
public record TransferCompleted(Transaction transaction, Origin origin) {

    /** Transferi neyin başlattığı; aynı olay için iki bildirim gönderilmesini önler. */
    public enum Origin { DIRECT, PAYMENT_REQUEST, SCHEDULED }

    public boolean internal() {
        return transaction.getSenderAccount().getUser().getId().equals(transaction.getReceiverAccount().getUser().getId());
    }
}
