package p2p_transfer.account;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import p2p_transfer.account.AccountDtos.OpenAccountRequest;
import p2p_transfer.common.Iban;
import p2p_transfer.common.error.BusinessException;
import p2p_transfer.common.error.ErrorCode;
import p2p_transfer.config.AppProperties;
import p2p_transfer.user.User;
import p2p_transfer.user.UserRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Map;

@Service
public class AccountService {

    static final String DEFAULT_ACCOUNT_NAME = "Vadesiz TL Hesabı";
    private static final int MAX_IBAN_ATTEMPTS = 10;

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final AppProperties props;
    private final Clock clock;

    public AccountService(AccountRepository accountRepository, UserRepository userRepository,
                          AppProperties props, Clock clock) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.props = props;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Account> listForUser(Long userId) {
        return accountRepository.findByUserIdOrderByIdAsc(userId);
    }

    /** Başka kullanıcının hesabı için de "bulunamadı" döner: hesabın varlığı sızdırılmaz. */
    @Transactional(readOnly = true)
    public Account getOwned(Long userId, Long accountId) {
        return accountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));
    }

    /**
     * Giriş yapmış kullanıcı adına yeni hesap açar. Sahip oturumdaki kullanıcıdır, bakiye her zaman 0'dır
     * ve IBAN sunucuda üretilir (istemci IBAN veya bakiye belirleyemez).
     */
    @Transactional
    public Account open(Long userId, OpenAccountRequest request) {
        long count = accountRepository.countByUserId(userId);
        int max = props.bank().maxAccountsPerUser();
        if (count >= max) {
            throw new BusinessException(ErrorCode.ACCOUNT_LIMIT_REACHED, Map.of("max", max));
        }
        return create(userRepository.getReferenceById(userId), request.name().trim(), request.currency().name(), BigDecimal.ZERO);
    }

    /** Kayıt sırasında açılan ilk vadesiz TL hesabı. */
    @Transactional
    public Account openInitial(User user) {
        return create(user, DEFAULT_ACCOUNT_NAME, "TRY", props.onboarding().welcomeBalance());
    }

    @Transactional
    public Account rename(Long userId, Long accountId, String name) {
        Account account = getOwned(userId, accountId);
        account.setName(name.trim());
        return account;
    }

    @Transactional(readOnly = true)
    public Account lookupByIban(String rawIban) {
        return accountRepository.findByIban(validIban(rawIban))
                .orElseThrow(() -> new BusinessException(ErrorCode.IBAN_NOT_FOUND));
    }

    /** Transfer için: entity yüklemeden yalnızca hesap id'sini çözer (bkz. AccountRepository#findIdByIban). */
    @Transactional(readOnly = true)
    public Long resolveIdByIban(String rawIban) {
        return accountRepository.findIdByIban(validIban(rawIban))
                .orElseThrow(() -> new BusinessException(ErrorCode.IBAN_NOT_FOUND));
    }

    private static String validIban(String rawIban) {
        String iban = Iban.normalize(rawIban);
        if (!Iban.isTrFormat(iban)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    Map.of("errors", Map.of("iban", "Geçerli bir TR IBAN giriniz.")));
        }
        return iban;
    }

    private Account create(User owner, String name, String currency, BigDecimal balance) {
        Account account = new Account();
        account.setUser(owner);
        account.setIban(uniqueIban());
        account.setName(name);
        account.setCurrency(currency);
        account.setBalance(balance);
        account.setCreatedAt(clock.instant());
        return accountRepository.save(account);
    }

    private String uniqueIban() {
        for (int i = 0; i < MAX_IBAN_ATTEMPTS; i++) {
            String iban = Iban.generate(props.bank().code());
            if (!accountRepository.existsByIban(iban)) {
                return iban;
            }
        }
        throw new IllegalStateException("Could not allocate a unique IBAN");
    }
}
