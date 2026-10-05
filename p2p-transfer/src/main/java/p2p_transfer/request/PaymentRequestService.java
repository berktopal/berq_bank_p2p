package p2p_transfer.request;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import p2p_transfer.account.Account;
import p2p_transfer.account.AccountService;
import p2p_transfer.common.PageResponse;
import p2p_transfer.common.error.BusinessException;
import p2p_transfer.common.error.ErrorCode;
import p2p_transfer.config.FeatureProperties;
import p2p_transfer.notification.NotificationService;
import p2p_transfer.notification.NotificationType;
import p2p_transfer.request.PaymentRequest.Status;
import p2p_transfer.request.PaymentRequestDtos.CreatePaymentRequest;
import p2p_transfer.request.PaymentRequestDtos.PaymentRequestResponse;
import p2p_transfer.request.PaymentRequestDtos.Role;
import p2p_transfer.transfer.TransactionCategory;
import p2p_transfer.transfer.TransferCompleted;
import p2p_transfer.transfer.TransferDtos.TransferRequest;
import p2p_transfer.transfer.TransferService;
import p2p_transfer.user.User;
import p2p_transfer.user.UserRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

@Slf4j
@Service
public class PaymentRequestService {

    private final PaymentRequestRepository requests;
    private final AccountService accountService;
    private final TransferService transferService;
    private final NotificationService notifications;
    private final UserRepository users;
    private final FeatureProperties.PaymentRequests props;
    private final Clock clock;

    public PaymentRequestService(PaymentRequestRepository requests, AccountService accountService,
                                 TransferService transferService, NotificationService notifications,
                                 UserRepository users, FeatureProperties.PaymentRequests props, Clock clock) {
        this.requests = requests;
        this.accountService = accountService;
        this.transferService = transferService;
        this.notifications = notifications;
        this.users = users;
        this.props = props;
        this.clock = clock;
    }

    /** Ödeyen IBAN'ından bulunur; para isteyenin seçtiği kendi hesabına yatacaktır. */
    @Transactional
    public PaymentRequestResponse create(Long userId, CreatePaymentRequest body) {
        Account toAccount = accountService.getOwned(userId, body.toAccountId());
        Account payerAccount = accountService.lookupByIban(body.payerIban());
        if (payerAccount.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.REQUEST_SELF);
        }
        Instant now = clock.instant();
        User requester = users.getReferenceById(userId);

        PaymentRequest r = new PaymentRequest();
        r.setRequester(requester);
        r.setRequesterAccount(toAccount);
        r.setPayer(payerAccount.getUser());
        r.setAmount(body.amount());
        r.setCurrency(toAccount.getCurrency());
        r.setDescription(body.description() == null || body.description().isBlank() ? null : body.description().trim());
        r.setStatus(Status.PENDING);
        r.setCreatedAt(now);
        r.setExpiresAt(now.plus(props.expiry()));
        requests.save(r);

        notifications.notify(r.getPayer().getId(), NotificationType.PAYMENT_REQUEST_RECEIVED, n -> {
            n.setAmount(r.getAmount());
            n.setCurrency(r.getCurrency());
            n.setCounterpartyName(r.getRequester().fullName());
            n.setPaymentRequestId(r.getId());
        });
        return PaymentRequestResponse.of(r, userId, now);
    }

    @Transactional(readOnly = true)
    public PageResponse<PaymentRequestResponse> list(Long userId, Role role, Status status, int page, int size) {
        Set<Status> statuses = status == null ? EnumSet.allOf(Status.class) : EnumSet.of(status);
        Instant now = clock.instant();
        var result = requests.findForUser(userId, (role == null ? Role.ALL : role).name(), statuses,
                PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 50)));
        return PageResponse.of(result, r -> PaymentRequestResponse.of(r, userId, now));
    }

    @Transactional(readOnly = true)
    public long pendingIncomingCount(Long userId) {
        return requests.countPendingIncoming(userId, clock.instant());
    }

    /**
     * İsteği öder. İstek satırı kilitlenir; transfer, isteğe özgü sabit bir idempotency anahtarıyla yapılır.
     * Böylece eşzamanlı iki "Öde" tıklaması da, yarıda kalan bir denemenin tekrarı da parayı bir kez taşır.
     */
    @Transactional
    public PaymentRequestResponse pay(Long userId, Long requestId, Long fromAccountId) {
        Instant now = clock.instant();
        PaymentRequest r = lockPending(requestId, userId, true, now);

        var result = transferService.transfer(userId,
                new TransferRequest(fromAccountId, r.getRequesterAccount().getIban(), r.getAmount(), r.getDescription(),
                        TransactionCategory.GENERAL),
                "preq-" + r.getId(), TransferCompleted.Origin.PAYMENT_REQUEST);

        r.setStatus(Status.PAID);
        r.setTransaction(result.transaction());
        r.setRespondedAt(now);
        notifications.notify(r.getRequester().getId(), NotificationType.PAYMENT_REQUEST_PAID, n -> {
            n.setAmount(r.getAmount());
            n.setCurrency(r.getCurrency());
            n.setCounterpartyName(r.getPayer().fullName());
            n.setPaymentRequestId(r.getId());
            n.setTransactionId(result.transaction().getId());
        });
        return PaymentRequestResponse.of(r, userId, now);
    }

    @Transactional
    public PaymentRequestResponse decline(Long userId, Long requestId) {
        Instant now = clock.instant();
        PaymentRequest r = lockPending(requestId, userId, true, now);
        r.setStatus(Status.DECLINED);
        r.setRespondedAt(now);
        notifications.notify(r.getRequester().getId(), NotificationType.PAYMENT_REQUEST_DECLINED, n -> {
            n.setAmount(r.getAmount());
            n.setCurrency(r.getCurrency());
            n.setCounterpartyName(r.getPayer().fullName());
            n.setPaymentRequestId(r.getId());
        });
        return PaymentRequestResponse.of(r, userId, now);
    }

    @Transactional
    public PaymentRequestResponse cancel(Long userId, Long requestId) {
        Instant now = clock.instant();
        PaymentRequest r = lockPending(requestId, userId, false, now);
        r.setStatus(Status.CANCELLED);
        r.setRespondedAt(now);
        notifications.notify(r.getPayer().getId(), NotificationType.PAYMENT_REQUEST_CANCELLED, n -> {
            n.setAmount(r.getAmount());
            n.setCurrency(r.getCurrency());
            n.setCounterpartyName(r.getRequester().fullName());
            n.setPaymentRequestId(r.getId());
        });
        return PaymentRequestResponse.of(r, userId, now);
    }

    /** Süresi dolan istekleri kapatır (listeler bunu zaten effectiveStatus ile gösterir; bu, veriyi tutarlı kılar). */
    @Scheduled(fixedDelay = 300_000, initialDelay = 60_000)
    @Transactional
    public void expireOverdue() {
        int expired = requests.expireOverdue(clock.instant());
        if (expired > 0) {
            log.info("{} payment request(s) expired", expired);
        }
    }

    /**
     * @param asPayer true: yalnızca ödeyen işlem yapabilir (öde/reddet); false: yalnızca isteyen (iptal)
     */
    private PaymentRequest lockPending(Long requestId, Long userId, boolean asPayer, Instant now) {
        PaymentRequest r = requests.findByIdForUpdate(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REQUEST_NOT_FOUND));
        Long actor = asPayer ? r.getPayer().getId() : r.getRequester().getId();
        // Yetkisiz kullanıcıya isteğin varlığı da sızdırılmaz
        if (!actor.equals(userId)) {
            throw new BusinessException(ErrorCode.REQUEST_NOT_FOUND);
        }
        if (r.effectiveStatus(now) != Status.PENDING) {
            throw new BusinessException(ErrorCode.REQUEST_NOT_PENDING);
        }
        return r;
    }
}
