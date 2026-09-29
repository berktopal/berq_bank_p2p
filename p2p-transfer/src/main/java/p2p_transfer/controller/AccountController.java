package p2p_transfer.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import p2p_transfer.dto.ApiViews.AccountLookupView;
import p2p_transfer.dto.ApiViews.AccountView;
import p2p_transfer.entity.Account;
import p2p_transfer.security.SessionAuth;
import p2p_transfer.service.AccountService;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    // Artık tüm hesapları değil, sadece giriş yapan kullanıcının hesaplarını döner
    @GetMapping
    public List<AccountView> getMyAccounts(HttpServletRequest request) {
        Long userId = SessionAuth.requireUserId(request);
        return accountService.getAccountsByUserId(userId).stream().map(AccountView::of).toList();
    }

    // IBAN sorgulama: sadece hesap ID'si ve maskelenmiş isim döner (TCKN, e-posta, bakiye yok)
    @GetMapping("/iban/{iban}")
    public AccountLookupView getByIban(@PathVariable String iban, HttpServletRequest request) {
        SessionAuth.requireUserId(request);
        return AccountLookupView.of(accountService.getAccountByIban(iban));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountView createAccount(@RequestBody Account account, HttpServletRequest request) {
        Long userId = SessionAuth.requireUserId(request);
        return AccountView.of(accountService.createAccount(userId, account));
    }

    // Kullanıcı yalnızca kendi hesaplarını görebilir
    @GetMapping("/user/{userId}")
    public List<AccountView> getAccounts(@PathVariable Long userId, HttpServletRequest request) {
        Long currentUserId = SessionAuth.requireUserId(request);
        if (!currentUserId.equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Başka bir kullanıcının hesaplarını göremezsiniz.");
        }
        return accountService.getAccountsByUserId(userId).stream().map(AccountView::of).toList();
    }
}
