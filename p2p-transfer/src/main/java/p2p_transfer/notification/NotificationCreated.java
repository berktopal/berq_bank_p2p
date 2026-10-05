package p2p_transfer.notification;

/** Bildirim veritabanına yazıldı; commit'ten sonra canlı akışa iletilir (bkz. NotificationStreams). */
public record NotificationCreated(Long userId, NotificationResponse notification) {
}
