package p2p_transfer.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import p2p_transfer.account.AccountService;
import p2p_transfer.auth.AuthDtos.RegisterRequest;
import p2p_transfer.common.error.BusinessException;
import p2p_transfer.common.error.ErrorCode;
import p2p_transfer.config.AppProperties;
import p2p_transfer.user.User;
import p2p_transfer.user.UserRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final AccountService accountService;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties props;
    private final Clock clock;

    /**
     * Var olmayan e-postalar için de bir BCrypt karşılaştırması yapılır; böylece yanıt süresi
     * "böyle bir kullanıcı var mı" sorusunu ele vermez (user enumeration / timing attack).
     */
    private final String dummyHash;

    public AuthService(UserRepository userRepository, AccountService accountService,
                       PasswordEncoder passwordEncoder, AppProperties props, Clock clock) {
        this.userRepository = userRepository;
        this.accountService = accountService;
        this.passwordEncoder = passwordEncoder;
        this.props = props;
        this.clock = clock;
        this.dummyHash = passwordEncoder.encode("timing-equalizer-not-a-real-password");
    }

    /** Kullanıcıyı oluşturur ve ilk vadesiz TL hesabını açar. */
    @Transactional
    public User register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException(ErrorCode.EMAIL_TAKEN);
        }
        if (userRepository.existsByTckn(request.tckn())) {
            throw new BusinessException(ErrorCode.TCKN_TAKEN);
        }
        User user = new User();
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setTckn(request.tckn());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setCreatedAt(clock.instant());
        user = userRepository.save(user);
        accountService.openInitial(user);
        return user;
    }

    /**
     * Kimlik doğrulama. Hatalı denemeler sayılır ve eşik aşılınca hesap geçici olarak kilitlenir.
     * noRollbackFor: hata fırlatsak da sayaç güncellemesi veritabanına yazılmalı.
     */
    @Transactional(noRollbackFor = BusinessException.class)
    public User authenticate(String rawEmail, String password) {
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(rawEmail)).orElse(null);
        if (user == null) {
            passwordEncoder.matches(password, dummyHash);
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        Instant now = clock.instant();
        if (user.isLocked(now)) {
            throw new BusinessException(ErrorCode.USER_LOCKED, Map.of("lockedUntil", user.getLockedUntil().toString()));
        }

        if (!passwordMatches(user, password)) {
            registerFailure(user, now);
        }

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        return user;
    }

    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (!passwordMatches(user, currentPassword)) {
            throw new BusinessException(ErrorCode.WRONG_CURRENT_PASSWORD);
        }
        user.setPassword(passwordEncoder.encode(newPassword));
    }

    private void registerFailure(User user, Instant now) {
        int attempts = user.getFailedLoginAttempts() + 1;
        if (attempts >= props.security().maxFailedLogins()) {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(now.plus(props.security().lockDuration()));
            throw new BusinessException(ErrorCode.USER_LOCKED, Map.of("lockedUntil", user.getLockedUntil().toString()));
        }
        user.setFailedLoginAttempts(attempts);
        throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
    }

    private boolean passwordMatches(User user, String password) {
        String stored = user.getPassword();
        if (isBcryptHash(stored)) {
            return passwordEncoder.matches(password, stored);
        }
        // Eski (düz metin) kayıtlar için geriye dönük uyumluluk:
        // doğru girilirse şifre hemen BCrypt'e yükseltilir.
        boolean valid = stored != null && MessageDigest.isEqual(
                stored.getBytes(StandardCharsets.UTF_8), password.getBytes(StandardCharsets.UTF_8));
        if (valid) {
            user.setPassword(passwordEncoder.encode(password));
        }
        return valid;
    }

    private static boolean isBcryptHash(String value) {
        return value != null && value.matches("^\\$2[aby]?\\$\\d{2}\\$.{53}$");
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
