package p2p_transfer.service;

import org.springframework.stereotype.Service;
import p2p_transfer.entity.Account;
import p2p_transfer.repository.AccountRepository;
import java.util.List;

@Service
public class AccountService {
    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    public Account createAccount(Account account) {
        return accountRepository.save(account);
    }

    public List<Account> getAccountsByUserId(Long userId) {
        return accountRepository.findByUserId(userId);
    }

    // IBAN ile hesap getirme (Yeni eklendi)
    public Account getAccountByIban(String iban) {
        return accountRepository.findByIban(iban)
                .orElseThrow(() -> new RuntimeException("Bu IBAN'a ait bir hesap bulunamadı!"));
    }
}