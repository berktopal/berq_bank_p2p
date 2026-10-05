package p2p_transfer.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import p2p_transfer.account.AccountService;
import p2p_transfer.common.error.BusinessException;
import p2p_transfer.common.error.ErrorCode;
import p2p_transfer.config.AppProperties;
import p2p_transfer.user.User;
import p2p_transfer.user.UserRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Giriş kilidinin zamana bağlı davranışı: ayarlanabilir saatle, veritabanı olmadan. */
class AuthServiceTest {

    private static final Duration LOCK = Duration.ofMinutes(15);

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final UserRepository users = mock(UserRepository.class);
    private final MutableClock clock = new MutableClock(Instant.parse("2026-03-01T10:00:00Z"));
    private AuthService service;
    private User user;

    @BeforeEach
    void setUp() {
        var props = new AppProperties(new AppProperties.Bank("00999", ZoneId.of("Europe/Istanbul"), 5),
                new AppProperties.Transfer(new BigDecimal("50000")),
                new AppProperties.Security(3, LOCK),
                new AppProperties.Onboarding(BigDecimal.ZERO));
        service = new AuthService(users, mock(AccountService.class), encoder, props, clock);

        user = new User();
        user.setId(1L);
        user.setEmail("ada@test.local");
        user.setPassword(encoder.encode("Correct123"));
        when(users.findByEmailIgnoreCase("ada@test.local")).thenReturn(Optional.of(user));
    }

    @Test
    void locksAfterConfiguredNumberOfFailuresAndUnlocksWhenTimeElapses() {
        assertCode(() -> service.authenticate("ada@test.local", "nope"), ErrorCode.INVALID_CREDENTIALS);
        assertCode(() -> service.authenticate("ada@test.local", "nope"), ErrorCode.INVALID_CREDENTIALS);
        assertCode(() -> service.authenticate("ada@test.local", "nope"), ErrorCode.USER_LOCKED);

        // Kilit süresince doğru şifre de reddedilir
        assertCode(() -> service.authenticate("ada@test.local", "Correct123"), ErrorCode.USER_LOCKED);

        clock.advance(LOCK.plusSeconds(1));
        assertThat(service.authenticate("ada@test.local", "Correct123").getId()).isEqualTo(1L);
        assertThat(user.getFailedLoginAttempts()).isZero();
        assertThat(user.getLockedUntil()).isNull();
    }

    @Test
    void successfulLoginResetsFailureCounter() {
        assertCode(() -> service.authenticate("ada@test.local", "nope"), ErrorCode.INVALID_CREDENTIALS);
        service.authenticate("ADA@test.local ", "Correct123");
        assertThat(user.getFailedLoginAttempts()).isZero();
    }

    @Test
    void unknownEmailGetsTheSameErrorAsWrongPassword() {
        assertCode(() -> service.authenticate("ghost@test.local", "whatever"), ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    void legacyPlaintextPasswordIsUpgradedToBcrypt() {
        user.setPassword("legacyPass1");
        service.authenticate("ada@test.local", "legacyPass1");
        assertThat(user.getPassword()).startsWith("$2");
        assertThat(encoder.matches("legacyPass1", user.getPassword())).isTrue();
    }

    private static void assertCode(Runnable call, ErrorCode expected) {
        assertThatThrownBy(call::run).isInstanceOfSatisfying(BusinessException.class,
                e -> assertThat(e.code()).isEqualTo(expected));
    }

    private static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

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
