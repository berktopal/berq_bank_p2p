package p2p_transfer.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import p2p_transfer.transfer.TransactionCategory;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Kullanıcıya gösterilecek olay. Metin saklanmaz; arayüz {@link NotificationType} ve alanlardan kendi dilinde cümle kurar.
 * İlişkiler yalnızca id olarak tutulur: bildirim listesi başka tablolara join gerektirmez.
 */
@Getter
@Setter
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private NotificationType type;

    @Column(precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(length = 3)
    private String currency;

    @Column(length = 120)
    private String counterpartyName;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private TransactionCategory category;

    private Long transactionId;

    private Long paymentRequestId;

    private Long scheduledTransferId;

    @Column(length = 40)
    private String errorCode;

    private Instant readAt;

    @Column(nullable = false)
    private Instant createdAt;
}
