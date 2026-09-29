package p2p_transfer.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import p2p_transfer.entity.Account;
import p2p_transfer.repository.AccountRepository;
import p2p_transfer.repository.UserRepository;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AccountService {
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    public AccountService(AccountRepository accountRepository, UserRepository userRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }

    /**
     * Giriş yapmış kullanıcı adına yeni hesap açar.
     * Hesap sahibi ve başlangıç bakiyesi istemciden alınmaz: sahip oturumdaki
     * kullanıcıdır, bakiye her zaman 0'dır (aksi halde herkes kendine para yaratabilirdi).
     */
    @Transactional
    public Account createAccount(Long ownerUserId, Account request) {
        String iban = request.getIban() == null ? null : request.getIban().replaceAll("\\s+", "").toUpperCase();
        if (iban == null || !iban.matches("^TR\\d{24}$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Geçerli bir TR IBAN giriniz.");
        }
        if (accountRepository.findByIban(iban).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bu IBAN zaten kullanılıyor.");
        }
        Account account = new Account();
        account.setUser(userRepository.getReferenceById(ownerUserId));
        account.setIban(iban);
        account.setBalance(BigDecimal.ZERO);
        account.setCurrency(request.getCurrency() == null || request.getCurrency().isBlank()
                ? "TRY" : request.getCurrency().trim().toUpperCase());
        return accountRepository.save(account);
    }

    @Transactional(readOnly = true)
    public List<Account> getAccountsByUserId(Long userId) {
        return accountRepository.findByUserId(userId);
    }

    // IBAN ile hesap getirme (alıcı adını göstermek için)
    @Transactional(readOnly = true)
    public Account getAccountByIban(String iban) {
        String normalized = iban == null ? "" : iban.replaceAll("\\s+", "").toUpperCase();
        Account account = accountRepository.findByIban(normalized)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bu IBAN'a ait bir hesap bulunamadı!"));
        account.getUser().getFirstName(); // lazy ilişkiyi transaction içinde yükle
        return account;
    }
}
