package p2p_transfer.budget;

import org.springframework.data.jpa.repository.JpaRepository;
import p2p_transfer.transfer.TransactionCategory;

import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    List<Budget> findByUserIdOrderByCurrencyAscCategoryAsc(Long userId);

    Optional<Budget> findByUserIdAndCategoryAndCurrency(Long userId, TransactionCategory category, String currency);

    Optional<Budget> findByIdAndUserId(Long id, Long userId);
}
