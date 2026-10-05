package p2p_transfer.analytics;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import p2p_transfer.account.Account;
import p2p_transfer.account.AccountService;
import p2p_transfer.config.AppProperties;
import p2p_transfer.transfer.TransactionCategory;
import p2p_transfer.transfer.TransactionRepository;
import p2p_transfer.transfer.TransactionRepository.MonthlyFlow;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    /** Kendi hesaplarına virmanlar hariç; isimler işlem geçmişiyle tutarlı olarak tam gösterilir. */
    private static final int TOP_RECIPIENTS = 5;

    public record MonthPoint(String month, BigDecimal incoming, BigDecimal outgoing) {
    }

    public record CategoryPoint(TransactionCategory category, BigDecimal total, long count) {
    }

    public record RecipientPoint(String name, String iban, BigDecimal total, long count) {
    }

    public record Summary(Long accountId, String currency, Instant since, BigDecimal totalIncoming,
                          BigDecimal totalOutgoing, BigDecimal net, List<MonthPoint> monthly,
                          List<CategoryPoint> byCategory, List<RecipientPoint> topRecipients) {
    }

    private final TransactionRepository transactionRepository;
    private final AccountService accountService;
    private final AppProperties props;
    private final Clock clock;

    public AnalyticsService(TransactionRepository transactionRepository, AccountService accountService,
                            AppProperties props, Clock clock) {
        this.transactionRepository = transactionRepository;
        this.accountService = accountService;
        this.props = props;
        this.clock = clock;
    }

    /** Son {@code months} takvim ayı (içinde bulunulan ay dahil) için hesap özeti. */
    @Transactional(readOnly = true)
    public Summary summary(Long userId, Long accountId, int months) {
        Account account = accountService.getOwned(userId, accountId);
        ZoneId zone = props.bank().zone();
        YearMonth current = YearMonth.now(clock.withZone(zone));
        YearMonth first = current.minusMonths(months - 1L);
        Instant since = first.atDay(1).atStartOfDay(zone).toInstant();

        Map<String, MonthlyFlow> byMonth = transactionRepository.monthlyFlows(accountId, since, zone.getId()).stream()
                .collect(Collectors.toMap(MonthlyFlow::getMonth, Function.identity()));

        // Hareket olmayan aylar da grafikte 0 olarak görünmeli
        List<MonthPoint> monthly = new ArrayList<>(months);
        BigDecimal in = BigDecimal.ZERO;
        BigDecimal out = BigDecimal.ZERO;
        for (YearMonth m = first; !m.isAfter(current); m = m.plusMonths(1)) {
            MonthlyFlow f = byMonth.get(m.toString());
            BigDecimal mi = f == null ? BigDecimal.ZERO : f.getIncoming();
            BigDecimal mo = f == null ? BigDecimal.ZERO : f.getOutgoing();
            monthly.add(new MonthPoint(m.toString(), mi, mo));
            in = in.add(mi);
            out = out.add(mo);
        }

        List<CategoryPoint> categories = transactionRepository.spendingByCategory(accountId, since).stream()
                .map(c -> new CategoryPoint(c.getCategory(), c.getTotal(), c.getCount()))
                .toList();

        List<RecipientPoint> recipients = transactionRepository
                .topRecipients(accountId, since, PageRequest.of(0, TOP_RECIPIENTS)).stream()
                .map(r -> new RecipientPoint(r.getFirstName() + " " + r.getLastName(), r.getIban(),
                        r.getTotal(), r.getCount()))
                .toList();

        return new Summary(accountId, account.getCurrency(), since, in, out, in.subtract(out), monthly, categories, recipients);
    }
}
