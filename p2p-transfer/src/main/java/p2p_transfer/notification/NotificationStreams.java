package p2p_transfer.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Açık Server-Sent Events bağlantıları. Bir kullanıcının birden çok sekmesi olabilir; her biri ayrı emitter'dır.
 * Tek sunucu için bellek içi tutulur; yatay ölçeklenirse olaylar Redis Pub/Sub gibi bir kanaldan dağıtılmalıdır.
 */
@Slf4j
@Component
public class NotificationStreams {

    private static final long TIMEOUT_MILLIS = Duration.ofMinutes(30).toMillis();

    private final Map<Long, Set<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MILLIS);
        Set<SseEmitter> set = emitters.computeIfAbsent(userId, id -> new CopyOnWriteArraySet<>());
        set.add(emitter);
        Runnable remove = () -> set.remove(emitter);
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(e -> remove.run());
        // İlk olay: tarayıcı bağlantının kurulduğunu bilsin, proxy'ler de başlıkları hemen iletsin
        send(emitter, SseEmitter.event().name("ready").data("ok"), set);
        return emitter;
    }

    /** fallbackExecution: transaction dışında yayınlanan bildirimler de iletilsin. */
    @TransactionalEventListener(fallbackExecution = true)
    public void onNotification(NotificationCreated event) {
        Set<SseEmitter> set = emitters.get(event.userId());
        if (set == null) {
            return;
        }
        set.forEach(e -> send(e, SseEmitter.event()
                .name("notification")
                .id(String.valueOf(event.notification().id()))
                .data(event.notification()), set));
    }

    /** Boşta kalan bağlantılar proxy/yük dengeleyici tarafından kesilmesin diye yorum satırı gönderir. */
    @Scheduled(fixedRate = 25_000)
    public void heartbeat() {
        emitters.values().forEach(set -> set.forEach(e -> send(e, SseEmitter.event().comment("ping"), set)));
    }

    public int connectionCount(Long userId) {
        Set<SseEmitter> set = emitters.get(userId);
        return set == null ? 0 : set.size();
    }

    private static void send(SseEmitter emitter, SseEmitter.SseEventBuilder event, Set<SseEmitter> owner) {
        try {
            emitter.send(event);
        } catch (IOException | IllegalStateException e) {
            // İstemci bağlantıyı kapatmış: listeden çıkar
            owner.remove(emitter);
            log.debug("SSE client disconnected: {}", e.getMessage());
        }
    }
}
