package p2p_transfer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import p2p_transfer.entity.Transaction;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // Sadece kullanıcının taraf olduğu işlemler (gönderen veya alıcı)
    @Query("""
            select t from Transaction t
            join fetch t.senderAccount s join fetch s.user su
            join fetch t.receiverAccount r join fetch r.user ru
            where su.id = :userId or ru.id = :userId
            order by t.transactionDate asc, t.id asc
            """)
    List<Transaction> findAllForUser(@Param("userId") Long userId);
}
