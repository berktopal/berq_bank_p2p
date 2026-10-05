package p2p_transfer.schedule;

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
import p2p_transfer.transfer.TransactionCategory;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;

/** İleri tarihli (tek seferlik) veya düzenli (haftalık/aylık) transfer talimatı. */
@Getter
@Setter
@Entity
@Table(name = "scheduled_transfers")
public class ScheduledTransfer {

    public enum Frequency { ONCE, WEEKLY, MONTHLY }

    public enum Status { ACTIVE, PAUSED, COMPLETED, CANCELLED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_account_id", nullable = false)
    private Account fromAccount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_account_id", nullable = false)
    private Account toAccount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(length = 140)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TransactionCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Frequency frequency;

    @Column(nullable = false)
    private int dayOfMonth;

    @Column(nullable = false)
    private LocalDate nextRunDate;

    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status;

    private Instant lastRunAt;

    @Column(length = 40)
    private String lastError;

    @Column(nullable = false)
    private int runCount;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * {@code after} gününden sonraki ilk çalışma günü. Aylık talimatlarda hedef gün korunur:
     * 31'inde başlayan talimat Şubat'ta 28/29'unda, Mart'ta yine 31'inde çalışır.
     */
    public LocalDate occurrenceAfter(LocalDate after) {
        LocalDate next = nextRunDate;
        while (!next.isAfter(after)) {
            next = switch (frequency) {
                case WEEKLY -> next.plusWeeks(1);
                case MONTHLY -> {
                    YearMonth month = YearMonth.from(next).plusMonths(1);
                    yield month.atDay(Math.min(dayOfMonth, month.lengthOfMonth()));
                }
                case ONCE -> throw new IllegalStateException("One-off schedules have no next occurrence");
            };
        }
        return next;
    }
}
