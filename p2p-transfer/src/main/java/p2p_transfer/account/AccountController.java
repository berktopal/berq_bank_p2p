package p2p_transfer.account;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import p2p_transfer.account.AccountDtos.AccountLookupResponse;
import p2p_transfer.account.AccountDtos.AccountResponse;
import p2p_transfer.account.AccountDtos.OpenAccountRequest;
import p2p_transfer.account.AccountDtos.RenameAccountRequest;
import p2p_transfer.auth.AuthUser;

import java.util.List;

@Tag(name = "Accounts")
@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @Operation(summary = "Giriş yapan kullanıcının hesapları")
    @GetMapping
    public List<AccountResponse> list(@AuthenticationPrincipal AuthUser me) {
        return accountService.listForUser(me.id()).stream().map(AccountResponse::of).toList();
    }

    @GetMapping("/{id}")
    public AccountResponse get(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return AccountResponse.of(accountService.getOwned(me.id(), id));
    }

    @Operation(summary = "Yeni hesap aç (bakiye 0, IBAN sunucuda üretilir)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse open(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody OpenAccountRequest request) {
        return AccountResponse.of(accountService.open(me.id(), request));
    }

    @PatchMapping("/{id}")
    public AccountResponse rename(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                                  @Valid @RequestBody RenameAccountRequest request) {
        return AccountResponse.of(accountService.rename(me.id(), id, request.name()));
    }

    @Operation(summary = "Alıcı doğrulama: IBAN → maskeli isim ve para birimi")
    @GetMapping("/lookup")
    public AccountLookupResponse lookup(@AuthenticationPrincipal AuthUser me, @RequestParam String iban) {
        return AccountLookupResponse.of(accountService.lookupByIban(iban), me.id());
    }
}
