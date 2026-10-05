package p2p_transfer.transfer;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.LockModeType;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import p2p_transfer.account.Account;
import p2p_transfer.account.AccountService;
import p2p_transfer.common.error.BusinessException;
import p2p_transfer.common.error.ErrorCode;
import p2p_transfer.config.AppProperties;
import p2p_transfer.transfer.TransferDtos.TransferRequest;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;

/**
 * Para transferi.
 * <pre>
 *  1. Alıcıyı IBAN ile bul, aynı hesaba transferi reddet
 *  2. İki hesabı da id sırasına göre kilitle (PESSIMISTIC_WRITE → double spend yok, sabit sıra → deadlock yok)
 *  3. Gönderen hesap kullanıcıya ait olmalı
 *  4. Aynı Idempotency-Key daha önce işlendiyse aynı sonucu döndür
 *  5. Para birimi, günlük limit ve bakiye kontrolleri
 *  6. Borç/alacak + dekont kaydı — hepsi tek bir veritabanı transaction'ında
 * </pre>
 */
@Service
public class TransferService {

    public record TransferResult(Transaction transaction, boolean replayed) {
    }

    private final AccountService accountService;
    private final TransactionRepository transactionRepository;
    private final ReferenceGenerator referenceGenerator;
    private final AppProperties props;
    private final Clock clock;
    private final ApplicationEventPublisher events;
    private final EntityManager entityManager;

    public TransferService(AccountService accountService,
                           TransactionRepository transactionRepository, ReferenceGenerator referenceGenerator,
                           AppProperties props, Clock clock, ApplicationEventPublisher events,
                           EntityManager entityManager) {
        this.accountService = accountService;
        this.transactionRepository = transactionRepository;
        this.referenceGenerator = referenceGenerator;
        this.props = props;
        this.clock = clock;
        this.events = events;
        this.entityManager = entityManager;
    }

    @Transactional
    public TransferResult transfer(Long userId, TransferRequest request, String idempotencyKey) {
        return transfer(userId, request, idempotencyKey, TransferCompleted.Origin.DIRECT);
    }

    /** Para isteği ödemesi ve düzenli talimatlar da aynı kurallardan geçer; yalnızca kaynakları farklıdır. */
    @Transactional
    public TransferResult transfer(Long userId, TransferRequest request, String idempotencyKey,
                                   TransferCompleted.Origin origin) {
        Long senderId = request.fromAccountId();
        Long receiverId = accountService.resolveIdByIban(request.toIban());
        if (senderId.equals(receiverId)) {
            throw new BusinessException(ErrorCode.SAME_ACCOUNT);
        }

        Account sender;
        Account receiver;
        if (senderId < receiverId) {
            sender = lock(senderId);
            receiver = lock(receiverId);
        } else {
            receiver = lock(receiverId);
            sender = lock(senderId);
        }

        // Başkasının hesabı için de "bulunamadı": hesabın varlığı sızdırılmaz
        if (!sender.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND);
        }

        // Gönderen satırı kilitli olduğu için aynı anahtarla gelen eşzamanlı istekler burada sıraya girer
        if (idempotencyKey != null) {
            Optional<Transaction> previous = transactionRepository.findBySenderAccountIdAndIdempotencyKey(senderId, idempotencyKey);
            if (previous.isPresent()) {
                Transaction prev = previous.get();
                // Aynı anahtar farklı bir istek için kullanılmışsa sessizce eski sonucu döndürmek yanıltıcı olur
                if (!prev.getReceiverAccount().getId().equals(receiverId) || prev.getAmount().compareTo(request.amount()) != 0) {
                    throw new BusinessException(ErrorCode.IDEMPOTENCY_CONFLICT);
                }
                return new TransferResult(prev, true);
            }
        }

        if (!sender.getCurrency().equals(receiver.getCurrency())) {
            throw new BusinessException(ErrorCode.CURRENCY_MISMATCH);
        }

        BigDecimal amount = request.amount();
        boolean toOwnAccount = receiver.isOwnedBy(userId);
        if (!toOwnAccount) {
            checkDailyLimit(sender, amount);
        }

        if (sender.getBalance().compareTo(amount) < 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_FUNDS, Map.of("balance", sender.getBalance()));
        }

        sender.setBalance(sender.getBalance().subtract(amount));
        receiver.setBalance(receiver.getBalance().add(amount));

        Transaction tx = new Transaction();
        tx.setReference(referenceGenerator.next());
        tx.setSenderAccount(sender);
        tx.setReceiverAccount(receiver);
        tx.setAmount(amount);
        tx.setCurrency(sender.getCurrency());
        tx.setDescription(blankToNull(request.description()));
        tx.setCategory(request.category() == null ? TransactionCategory.GENERAL : request.category());
        tx.setCreatedAt(clock.instant());
        tx.setStatus(TransactionStatus.SUCCESS);
        tx.setIdempotencyKey(idempotencyKey);
        tx.setSenderBalanceAfter(sender.getBalance());
        tx.setReceiverBalanceAfter(receiver.getBalance());

        Transaction saved = transactionRepository.save(tx);
        // Tekrar denemelerde (replay) yayınlanmaz: bildirim ve bütçe uyarısı yalnızca bir kez üretilir
        events.publishEvent(new TransferCompleted(saved, origin));
        return new TransferResult(saved, false);
    }

    /** Kendi hesapları arası virmanlar limite dahil değildir. Gün, bankanın saat dilimine göre hesaplanır. */
    private void checkDailyLimit(Account sender, BigDecimal amount) {
        BigDecimal limit = props.transfer().dailyLimit();
        ZoneId zone = props.bank().zone();
        Instant startOfDay = LocalDate.now(clock.withZone(zone)).atStartOfDay(zone).toInstant();
        BigDecimal used = transactionRepository.sumOutgoingToOthersSince(sender.getId(), startOfDay);
        if (used.add(amount).compareTo(limit) > 0) {
            throw new BusinessException(ErrorCode.DAILY_LIMIT_EXCEEDED,
                    Map.of("limit", limit, "remaining", limit.subtract(used).max(BigDecimal.ZERO)));
        }
    }

    /**
     * SELECT ... FOR UPDATE ile satırı kilitler ve durumu kilitli satırdan yeniden okur.
     * Sorgu tabanlı kilit yetmez: hesap bu transaction'da daha önce kilitsiz yüklendiyse (ör. para isteği
     * ödenirken IBAN'ı okunduğunda) Hibernate önbellekteki eski bakiyeyi döndürür ve eşzamanlı bir
     * alacak kaybolur. getReference sorgu atmaz; refresh tek sorguyla hem kilitler hem tazeler.
     */
    private Account lock(Long accountId) {
        try {
            Account account = entityManager.getReference(Account.class, accountId);
            entityManager.refresh(account, LockModeType.PESSIMISTIC_WRITE);
            return account;
        } catch (EntityNotFoundException e) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND);
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
