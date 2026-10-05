package p2p_transfer.ratelimit;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import p2p_transfer.config.FeatureProperties.RateLimit.Rule;

import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bellek içi token bucket. Her anahtar (ör. "login:203.0.113.7", "lookup:42") kendi kovasına sahiptir:
 * kova {@code capacity} jetonla dolu başlar ve {@code period} boyunca sürekli olarak yeniden dolar.
 * <p>
 * Tek sunucu için yeterlidir; yatay ölçeklenirse sayaçlar Redis gibi paylaşılan bir depoya taşınmalıdır.
 */
@Component
public class RateLimiter {

    public record Decision(boolean allowed, long retryAfterSeconds) {
    }

    private static final class Bucket {
        double tokens;
        long updatedAtMillis;

        Bucket(double tokens, long now) {
            this.tokens = tokens;
            this.updatedAtMillis = now;
        }
    }

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final Clock clock;

    public RateLimiter(Clock clock) {
        this.clock = clock;
    }

    public Decision tryConsume(String key, Rule rule) {
        long now = clock.millis();
        double perMilli = rule.capacity() / (double) rule.period().toMillis();
        Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket(rule.capacity(), now));
        synchronized (bucket) {
            long elapsed = Math.max(0, now - bucket.updatedAtMillis);
            bucket.tokens = Math.min(rule.capacity(), bucket.tokens + elapsed * perMilli);
            bucket.updatedAtMillis = now;
            if (bucket.tokens >= 1) {
                bucket.tokens -= 1;
                return new Decision(true, 0);
            }
            long waitMillis = (long) Math.ceil((1 - bucket.tokens) / perMilli);
            return new Decision(false, Math.max(1, (waitMillis + 999) / 1000));
        }
    }

    /** Uzun süredir kullanılmayan kovaları atar; bellek sınırsız büyümez. */
    @Scheduled(fixedRate = 600_000)
    public void evictIdle() {
        long cutoff = clock.millis() - 3_600_000;
        buckets.entrySet().removeIf(e -> e.getValue().updatedAtMillis < cutoff);
    }

    public void reset() {
        buckets.clear();
    }
}
