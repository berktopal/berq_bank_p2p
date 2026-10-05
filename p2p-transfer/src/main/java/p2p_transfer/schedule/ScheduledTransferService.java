package p2p_transfer.schedule;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import p2p_transfer.account.Account;
import p2p_transfer.account.AccountService;
import p2p_transfer.common.error.BusinessException;
import p2p_transfer.common.error.ErrorCode;
import p2p_transfer.config.AppProperties;
import p2p_transfer.notification.NotificationService;
import p2p_transfer.notification.NotificationType;
import p2p_transfer.schedule.ScheduledTransfer.Frequency;
import p2p_transfer.schedule.ScheduledTransfer.Status;
import p2p_transfer.schedule.ScheduledTransferDtos.CreateScheduleRequest;
import p2p_transfer.schedule.ScheduledTransferDtos.ScheduledTransferResponse;
import p2p_transfer.transfer.TransactionCategory;
import p2p_transfer.transfer.TransferCompleted;
import p2p_transfer.transfer.TransferDtos.TransferRequest;
import p2p_transfer.transfer.TransferService;
import p2p_transfer.transfer.TransferService.TransferResult;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * Düzenli ve ileri tarihli transferler.
 * <p>
 * Tam olarak bir kez çalışma garantisi iki katmandan gelir:
 * <ol>
 *   <li>Talimat satırı {@code FOR UPDATE SKIP LOCKED} ile alınır: iki sunucu aynı talimatı aynı anda işleyemez.</li>
 *   <li>Transfer {@code sched-<id>-<tarih>} idempotency anahtarıyla yapılır: transfer commit edildikten sonra
 *       süreç çökerse, bir sonraki turda aynı gün için para tekrar gönderilmez, mevcut kayıt döner.</li>
 * </ol>
 * Transfer ayrı bir transaction'da (REQUIRES_NEW) koşar; böylece yetersiz bakiye gibi bir hata
 * talimatın "başarısız" olarak işaretlenmesini engellemez.
 */
@Slf4j
@Service
public class ScheduledTransferService {

    private static final int MAX_PER_RUN = 200;

    private final ScheduledTransferRepository schedules;
    private final AccountService accountService;
    private final TransferService transferService;
    private final NotificationService notifications;
    private final AppProperties props;
    private final Clock clock;
    private final TransactionTemplate claimTx;
    private final TransactionTemplate transferTx;

    public ScheduledTransferService(ScheduledTransferRepository schedules, AccountService accountService,
                                    TransferService transferService, NotificationService notifications,
                                    AppProperties props, Clock clock, PlatformTransactionManager txManager) {
        this.schedules = schedules;
        this.accountService = accountService;
        this.transferService = transferService;
        this.notifications = notifications;
        this.props = props;
        this.clock = clock;
        this.claimTx = new TransactionTemplate(txManager);
        this.transferTx = new TransactionTemplate(txManager);
        this.transferTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Transactional
    public ScheduledTransferResponse create(Long userId, CreateScheduleRequest body) {
        Account from = accountService.getOwned(userId, body.fromAccountId());
        Account to = accountService.lookupByIban(body.toIban());
        if (from.getId().equals(to.getId())) {
            throw new BusinessException(ErrorCode.SAME_ACCOUNT);
        }
        if (!from.getCurrency().equals(to.getCurrency())) {
            throw new BusinessException(ErrorCode.CURRENCY_MISMATCH);
        }
        LocalDate today = today();
        LocalDate end = body.frequency() == Frequency.ONCE ? null : body.endDate();
        if (body.startDate().isBefore(today) || (end != null && end.isBefore(body.startDate()))) {
            throw new BusinessException(ErrorCode.SCHEDULE_INVALID_DATES);
        }

        ScheduledTransfer s = new ScheduledTransfer();
        s.setUserId(userId);
        s.setFromAccount(from);
        s.setToAccount(to);
        s.setAmount(body.amount());
        s.setDescription(body.description() == null || body.description().isBlank() ? null : body.description().trim());
        s.setCategory(body.category() == null ? TransactionCategory.GENERAL : body.category());
        s.setFrequency(body.frequency());
        s.setDayOfMonth(body.startDate().getDayOfMonth());
        s.setNextRunDate(body.startDate());
        s.setEndDate(end);
        s.setStatus(Status.ACTIVE);
        s.setCreatedAt(clock.instant());
        return ScheduledTransferResponse.of(schedules.save(s));
    }

    @Transactional(readOnly = true)
    public List<ScheduledTransferResponse> list(Long userId) {
        return schedules.findAllForUser(userId).stream().map(ScheduledTransferResponse::of).toList();
    }

    /** Duraklat / sürdür. Duraklatılmışken kaçırılan günler sürdürünce toplu çalıştırılmaz, atlanır. */
    @Transactional
    public ScheduledTransferResponse updateStatus(Long userId, Long id, Status target) {
        ScheduledTransfer s = getOwned(userId, id);
        boolean editable = s.getStatus() == Status.ACTIVE || s.getStatus() == Status.PAUSED;
        if (!editable || (target != Status.ACTIVE && target != Status.PAUSED)) {
            throw new BusinessException(ErrorCode.SCHEDULE_NOT_ACTIVE);
        }
        if (target == Status.ACTIVE && s.getNextRunDate().isBefore(today())) {
            s.setNextRunDate(s.getFrequency() == Frequency.ONCE ? today() : s.occurrenceAfter(today().minusDays(1)));
        }
        s.setStatus(target);
        return ScheduledTransferResponse.of(s);
    }

    @Transactional
    public void cancel(Long userId, Long id) {
        ScheduledTransfer s = getOwned(userId, id);
        if (s.getStatus() != Status.ACTIVE && s.getStatus() != Status.PAUSED) {
            throw new BusinessException(ErrorCode.SCHEDULE_NOT_ACTIVE);
        }
        s.setStatus(Status.CANCELLED);
    }

    /**
     * Vadesi gelmiş talimatları birer birer, her biri kendi transaction'ında çalıştırır.
     * @return işlenen talimat sayısı
     */
    public int runDue(Instant now) {
        LocalDate today = LocalDate.ofInstant(now, zone());
        int processed = 0;
        while (processed < MAX_PER_RUN) {
            Boolean claimed = claimTx.execute(status -> schedules.claimNextDue(today)
                    .map(s -> {
                        execute(s, today, now);
                        return true;
                    })
                    .orElse(false));
            if (!Boolean.TRUE.equals(claimed)) {
                break;
            }
            processed++;
        }
        return processed;
    }

    private void execute(ScheduledTransfer s, LocalDate today, Instant now) {
        String key = "sched-" + s.getId() + "-" + s.getNextRunDate();
        TransferRequest request = new TransferRequest(s.getFromAccount().getId(), s.getToAccount().getIban(),
                s.getAmount(), s.getDescription(), s.getCategory());
        String toName = ScheduledTransferResponse.of(s).toName();
        try {
            TransferResult result = transferTx.execute(status ->
                    transferService.transfer(s.getUserId(), request, key, TransferCompleted.Origin.SCHEDULED));
            s.setRunCount(s.getRunCount() + 1);
            s.setLastError(null);
            if (result != null && !result.replayed()) {
                notifications.notify(s.getUserId(), NotificationType.SCHEDULED_TRANSFER_EXECUTED, n -> {
                    n.setAmount(s.getAmount());
                    n.setCurrency(s.getFromAccount().getCurrency());
                    n.setCounterpartyName(toName);
                    n.setCategory(s.getCategory());
                    n.setTransactionId(result.transaction().getId());
                    n.setScheduledTransferId(s.getId());
                });
            }
        } catch (BusinessException e) {
            log.info("Scheduled transfer {} failed on {}: {}", s.getId(), s.getNextRunDate(), e.code());
            s.setLastError(e.code().name());
            notifications.notify(s.getUserId(), NotificationType.SCHEDULED_TRANSFER_FAILED, n -> {
                n.setAmount(s.getAmount());
                n.setCurrency(s.getFromAccount().getCurrency());
                n.setCounterpartyName(toName);
                n.setScheduledTransferId(s.getId());
                n.setErrorCode(e.code().name());
            });
        }
        s.setLastRunAt(now);
        advance(s, today);
    }

    /** Başarısız çalışmalar da ilerletilir: bakiye yetmediyse para bir sonraki dönemde tekrar denenir, her dakika değil. */
    private static void advance(ScheduledTransfer s, LocalDate today) {
        if (s.getFrequency() == Frequency.ONCE) {
            s.setStatus(Status.COMPLETED);
            return;
        }
        LocalDate next = s.occurrenceAfter(today);
        s.setNextRunDate(next);
        if (s.getEndDate() != null && next.isAfter(s.getEndDate())) {
            s.setStatus(Status.COMPLETED);
        }
    }

    private ScheduledTransfer getOwned(Long userId, Long id) {
        return schedules.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));
    }

    private ZoneId zone() {
        return props.bank().zone();
    }

    private LocalDate today() {
        return LocalDate.now(clock.withZone(zone()));
    }
}
