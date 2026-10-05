package p2p_transfer.request;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import p2p_transfer.account.Account;
import p2p_transfer.transfer.Transaction;
import p2p_transfer.user.User;

import java.math.BigDecimal;
import java.time.Instant;

/** Bir kullanıcının başka bir kullanıcıdan para talebi. Ödendiğinde normal bir transferle kapanır. */
@Getter
@Setter
@Entity
@Table(name = "payment_requests")
public class PaymentRequest {

    public enum Status { PENDING, PAID, DECLINED, CANCELLED, EXPIRED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    /** Paranın yatacağı, isteyene ait hesap. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_account_id", nullable = false)
    private Account requesterAccount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payer_id", nullable = false)
    private User payer;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(length = 140)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant respondedAt;

    /** Süresi dolmuş ama henüz zamanlanmış görevce kapatılmamış istekler de "beklemede değil" sayılır. */
    public Status effectiveStatus(Instant now) {
        return status == Status.PENDING && expiresAt.isBefore(now) ? Status.EXPIRED : status;
    }
}
