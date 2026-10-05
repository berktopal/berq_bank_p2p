package p2p_transfer.budget;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import p2p_transfer.common.error.BusinessException;
import p2p_transfer.common.error.ErrorCode;
import p2p_transfer.config.AppProperties;
import p2p_transfer.notification.NotificationService;
import p2p_transfer.notification.NotificationType;
import p2p_transfer.transfer.Transaction;
import p2p_transfer.transfer.TransactionCategory;
import p2p_transfer.transfer.TransactionRepository;
import p2p_transfer.transfer.TransactionRepository.CategoryTotal;
import p2p_transfer.transfer.TransferCompleted;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Aylık kategori bütçeleri. Harcama = kullanıcının başkalarına yaptığı başarılı transferler
 * (kendi hesapları arası virmanlar harcama sayılmaz).
 */
@Service
public class BudgetService {

    /** Bu oranı geçince "yaklaşıyorsunuz" uyarısı gider. */
    static final BigDecimal WARNING_RATIO = new BigDecimal("0.80");

    public enum Status { OK, WARNING, EXCEEDED }

    public record BudgetResponse(Long id, TransactionCategory category, String currency, BigDecimal monthlyLimit,
                                 BigDecimal spent, BigDecimal remaining, int percent, Status status) {
    }

    private final BudgetRepository budgets;
    private final TransactionRepository transactions;
    private final NotificationService notifications;
    private final AppProperties props;
    private final Clock clock;

    public BudgetService(BudgetRepository budgets, TransactionRepository transactions,
                         NotificationService notifications, AppProperties props, Clock clock) {
        this.budgets = budgets;
        this.transactions = transactions;
        this.notifications = notifications;
        this.props = props;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> list(Long userId) {
        List<Budget> all = budgets.findByUserIdOrderByCurrencyAscCategoryAsc(userId);
        Instant since = startOfMonth();
        // Para birimi başına tek sorgu
        Map<String, Map<TransactionCategory, BigDecimal>> spentByCurrency = all.stream()
                .map(Budget::getCurrency).distinct()
                .collect(Collectors.toMap(Function.identity(), c -> transactions
                        .spendingByCategoryForUser(userId, c, since).stream()
                        .collect(Collectors.toMap(CategoryTotal::getCategory, CategoryTotal::getTotal))));
        return all.stream()
                .map(b -> toResponse(b, spentByCurrency.get(b.getCurrency()).getOrDefault(b.getCategory(), BigDecimal.ZERO)))
                .toList();
    }

    /** Aynı kategori + para birimi için tek bütçe: varsa limiti günceller. */
    @Transactional
    public BudgetResponse upsert(Long userId, TransactionCategory category, String currency, BigDecimal limit) {
        Budget budget = budgets.findByUserIdAndCategoryAndCurrency(userId, category, currency).orElseGet(() -> {
            Budget b = new Budget();
            b.setUserId(userId);
            b.setCategory(category);
            b.setCurrency(currency);
            b.setCreatedAt(clock.instant());
            return b;
        });
        budget.setMonthlyLimit(limit);
        budgets.save(budget);
        return toResponse(budget, transactions.spentInCategory(userId, currency, category, startOfMonth()));
    }

    @Transactional
    public void delete(Long userId, Long id) {
        budgets.delete(budgets.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BUDGET_NOT_FOUND)));
    }

    /**
     * Transferle aynı transaction'da çalışır. Eşik yalnızca bu transferle aşıldıysa bildirim gider;
     * eşiğin üstündeki her harcamada tekrar tekrar uyarı gönderilmez.
     */
    @EventListener
    public void onTransfer(TransferCompleted event) {
        if (event.internal()) {
            return;
        }
        Transaction tx = event.transaction();
        Long userId = tx.getSenderAccount().getUser().getId();
        budgets.findByUserIdAndCategoryAndCurrency(userId, tx.getCategory(), tx.getCurrency()).ifPresent(budget -> {
            BigDecimal spent = transactions.spentInCategory(userId, tx.getCurrency(), tx.getCategory(), startOfMonth());
            BigDecimal before = spent.subtract(tx.getAmount());
            BigDecimal limit = budget.getMonthlyLimit();
            BigDecimal warning = limit.multiply(WARNING_RATIO);

            NotificationType type = null;
            if (before.compareTo(limit) < 0 && spent.compareTo(limit) >= 0) {
                type = NotificationType.BUDGET_EXCEEDED;
            } else if (before.compareTo(warning) < 0 && spent.compareTo(warning) >= 0) {
                type = NotificationType.BUDGET_WARNING;
            }
            if (type != null) {
                notifications.notify(userId, type, n -> {
                    n.setCategory(budget.getCategory());
                    n.setCurrency(budget.getCurrency());
                    n.setAmount(limit);
                    n.setTransactionId(tx.getId());
                });
            }
        });
    }

    private BudgetResponse toResponse(Budget b, BigDecimal spent) {
        BigDecimal limit = b.getMonthlyLimit();
        int percent = spent.multiply(BigDecimal.valueOf(100)).divide(limit, 0, RoundingMode.HALF_UP).intValue();
        Status status = spent.compareTo(limit) >= 0 ? Status.EXCEEDED
                : spent.compareTo(limit.multiply(WARNING_RATIO)) >= 0 ? Status.WARNING : Status.OK;
        return new BudgetResponse(b.getId(), b.getCategory(), b.getCurrency(), limit, spent,
                limit.subtract(spent).max(BigDecimal.ZERO), percent, status);
    }

    private Instant startOfMonth() {
        ZoneId zone = props.bank().zone();
        return YearMonth.now(clock.withZone(zone)).atDay(1).atStartOfDay(zone).toInstant();
    }
}
