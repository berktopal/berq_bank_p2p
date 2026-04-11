package p2p_transfer.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_account_id", nullable = false)
    private Account senderAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_account_id", nullable = false)
    private Account receiverAccount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDateTime transactionDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status; 
}