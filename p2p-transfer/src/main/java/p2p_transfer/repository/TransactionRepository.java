package p2p_transfer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import p2p_transfer.entity.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
}