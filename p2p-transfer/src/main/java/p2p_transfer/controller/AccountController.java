package p2p_transfer.controller;

import org.springframework.web.bind.annotation.*;
import p2p_transfer.entity.Account;
import p2p_transfer.service.AccountService;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public List<Account> getAllAccounts() {
        return accountService.getAllAccounts();
    }

    // IBAN sorgulama için (Frontend'de isim çıkarma özelliği için yeni eklendi)
    @GetMapping("/iban/{iban}")
    public Account getByIban(@PathVariable String iban) {
        return accountService.getAccountByIban(iban);
    }

    @PostMapping
    public Account createAccount(@RequestBody Account account) {
        return accountService.createAccount(account);
    }

    @GetMapping("/user/{userId}")
    public List<Account> getAccounts(@PathVariable Long userId) {
        return accountService.getAccountsByUserId(userId);
    }
}