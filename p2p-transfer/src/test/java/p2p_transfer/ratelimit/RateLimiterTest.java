package p2p_transfer.ratelimit;

import org.junit.jupiter.api.Test;
import p2p_transfer.config.FeatureProperties.RateLimit.Rule;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterTest {

    private final MutableClock clock = new MutableClock();
    private final RateLimiter limiter = new RateLimiter(clock);
    private final Rule fivePerMinute = new Rule(5, Duration.ofMinutes(1));

    @Test
    void allowsBurstUpToCapacityThenRejectsWithRetryHint() {
        for (int i = 0; i < 5; i++) {
            assertThat(limiter.tryConsume("login:1.2.3.4", fivePerMinute).allowed()).isTrue();
        }
        RateLimiter.Decision denied = limiter.tryConsume("login:1.2.3.4", fivePerMinute);
        assertThat(denied.allowed()).isFalse();
        // 5 jeton/dk → bir jeton 12 saniyede dolar
        assertThat(denied.retryAfterSeconds()).isEqualTo(12);
    }

    @Test
    void refillsContinuouslyOverTime() {
        for (int i = 0; i < 5; i++) {
            limiter.tryConsume("k", fivePerMinute);
        }
        clock.advance(Duration.ofSeconds(11));
        assertThat(limiter.tryConsume("k", fivePerMinute).allowed()).isFalse();
        clock.advance(Duration.ofSeconds(2));
        assertThat(limiter.tryConsume("k", fivePerMinute).allowed()).isTrue();
    }

    @Test
    void keysAreIndependent() {
        for (int i = 0; i < 5; i++) {
            limiter.tryConsume("lookup:1", fivePerMinute);
        }
        assertThat(limiter.tryConsume("lookup:1", fivePerMinute).allowed()).isFalse();
        assertThat(limiter.tryConsume("lookup:2", fivePerMinute).allowed()).isTrue();
    }

    @Test
    void idleBucketsAreEvicted() {
        limiter.tryConsume("old", fivePerMinute);
        for (int i = 0; i < 4; i++) {
            limiter.tryConsume("old", fivePerMinute);
        }
        clock.advance(Duration.ofHours(2));
        limiter.evictIdle();
        // Atılan kova tam kapasiteyle yeniden başlar
        for (int i = 0; i < 5; i++) {
            assertThat(limiter.tryConsume("old", fivePerMinute).allowed()).isTrue();
        }
    }

    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-03-01T10:00:00Z");

        void advance(Duration d) {
            now = now.plus(d);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(now, zone);
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
