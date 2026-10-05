package p2p_transfer.notification;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import p2p_transfer.common.PageResponse;
import p2p_transfer.common.error.BusinessException;
import p2p_transfer.common.error.ErrorCode;

import java.time.Clock;
import java.util.function.Consumer;

@Service
public class NotificationService {

    private final NotificationRepository repository;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public NotificationService(NotificationRepository repository, ApplicationEventPublisher events, Clock clock) {
        this.repository = repository;
        this.events = events;
        this.clock = clock;
    }

    /**
     * Çağıranın transaction'ına katılır: transfer geri alınırsa bildirim de yazılmaz.
     * Canlı akışa gönderim commit'ten sonra yapılır.
     */
    @Transactional
    public Notification notify(Long userId, NotificationType type, Consumer<Notification> details) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type);
        n.setCreatedAt(clock.instant());
        details.accept(n);
        repository.save(n);
        events.publishEvent(new NotificationCreated(userId, NotificationResponse.of(n)));
        return n;
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(Long userId, boolean unreadOnly, int page, int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 50));
        var result = unreadOnly
                ? repository.findByUserIdAndReadAtIsNullOrderByCreatedAtDescIdDesc(userId, pageable)
                : repository.findByUserIdOrderByCreatedAtDescIdDesc(userId, pageable);
        return PageResponse.of(result, NotificationResponse::of);
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return repository.countByUserIdAndReadAtIsNull(userId);
    }

    @Transactional
    public void markRead(Long userId, Long id) {
        Notification n = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));
        if (n.getReadAt() == null) {
            n.setReadAt(clock.instant());
        }
    }

    @Transactional
    public int markAllRead(Long userId) {
        return repository.markAllRead(userId, clock.instant());
    }
}
