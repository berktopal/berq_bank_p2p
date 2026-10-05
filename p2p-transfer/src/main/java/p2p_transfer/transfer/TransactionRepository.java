package p2p_transfer.transfer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    String SENDER_USER = "senderAccount.user";
    String RECEIVER_USER = "receiverAccount.user";

    /** Liste sorgularında her satır için ayrı sorgu atılmasını (N+1) engeller. */
    @Override
    @EntityGraph(attributePaths = {"senderAccount", SENDER_USER, "receiverAccount", RECEIVER_USER})
    Page<Transaction> findAll(Specification<Transaction> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"senderAccount", SENDER_USER, "receiverAccount", RECEIVER_USER})
    List<Transaction> findAll(Specification<Transaction> spec);

    @EntityGraph(attributePaths = {"senderAccount", SENDER_USER, "receiverAccount", RECEIVER_USER})
    @Query("""
            select t from Transaction t
            where t.id = :id and (t.senderAccount.user.id = :userId or t.receiverAccount.user.id = :userId)
            """)
    Optional<Transaction> findForParticipant(@Param("id") Long id, @Param("userId") Long userId);

    Optional<Transaction> findBySenderAccountIdAndIdempotencyKey(Long senderAccountId, String idempotencyKey);

    /** Günlük limit hesabı: hesabın verilen andan bu yana başka kullanıcılara yaptığı başarılı transferler. */
    @Query("""
            select coalesce(sum(t.amount), 0) from Transaction t
            where t.senderAccount.id = :accountId
              and t.receiverAccount.user.id <> t.senderAccount.user.id
              and t.status = p2p_transfer.transfer.TransactionStatus.SUCCESS
              and t.createdAt >= :since
            """)
    BigDecimal sumOutgoingToOthersSince(@Param("accountId") Long accountId, @Param("since") Instant since);

    // ---------- bütçe sorguları: kullanıcının tüm hesaplarından başkalarına yaptığı harcama ----------

    @Query("""
            select t.category as category, sum(t.amount) as total, count(t) as count
            from Transaction t
            where t.senderAccount.user.id = :userId
              and t.receiverAccount.user.id <> :userId
              and t.currency = :currency
              and t.status = p2p_transfer.transfer.TransactionStatus.SUCCESS
              and t.createdAt >= :since
            group by t.category
            """)
    List<CategoryTotal> spendingByCategoryForUser(@Param("userId") Long userId, @Param("currency") String currency,
                                                  @Param("since") Instant since);

    @Query("""
            select coalesce(sum(t.amount), 0) from Transaction t
            where t.senderAccount.user.id = :userId
              and t.receiverAccount.user.id <> :userId
              and t.currency = :currency
              and t.category = :category
              and t.status = p2p_transfer.transfer.TransactionStatus.SUCCESS
              and t.createdAt >= :since
            """)
    BigDecimal spentInCategory(@Param("userId") Long userId, @Param("currency") String currency,
                               @Param("category") TransactionCategory category, @Param("since") Instant since);

    // ---------- analiz sorguları ----------

    interface MonthlyFlow {
        String getMonth();

        BigDecimal getIncoming();

        BigDecimal getOutgoing();
    }

    @Query(value = """
            select to_char(date_trunc('month', t.transaction_date at time zone :zone), 'YYYY-MM') as month,
                   coalesce(sum(case when t.receiver_account_id = :accountId then t.amount end), 0) as incoming,
                   coalesce(sum(case when t.sender_account_id = :accountId then t.amount end), 0) as outgoing
            from transactions t
            where (t.sender_account_id = :accountId or t.receiver_account_id = :accountId)
              and t.status = 'SUCCESS'
              and t.transaction_date >= :since
            group by 1
            order by 1
            """, nativeQuery = true)
    List<MonthlyFlow> monthlyFlows(@Param("accountId") Long accountId, @Param("since") Instant since, @Param("zone") String zone);

    @Query("""
            select t.category as category, sum(t.amount) as total, count(t) as count
            from Transaction t
            where t.senderAccount.id = :accountId
              and t.status = p2p_transfer.transfer.TransactionStatus.SUCCESS
              and t.createdAt >= :since
            group by t.category
            order by sum(t.amount) desc
            """)
    List<CategoryTotal> spendingByCategory(@Param("accountId") Long accountId, @Param("since") Instant since);

    interface CategoryTotal {
        TransactionCategory getCategory();

        BigDecimal getTotal();

        long getCount();
    }

    @Query("""
            select r.iban as iban, u.firstName as firstName, u.lastName as lastName,
                   sum(t.amount) as total, count(t) as count
            from Transaction t join t.receiverAccount r join r.user u
            where t.senderAccount.id = :accountId
              and u.id <> t.senderAccount.user.id
              and t.status = p2p_transfer.transfer.TransactionStatus.SUCCESS
              and t.createdAt >= :since
            group by r.iban, u.firstName, u.lastName
            order by sum(t.amount) desc
            """)
    List<RecipientTotal> topRecipients(@Param("accountId") Long accountId, @Param("since") Instant since, Pageable limit);

    interface RecipientTotal {
        String getIban();

        String getFirstName();

        String getLastName();

        BigDecimal getTotal();

        long getCount();
    }
}
