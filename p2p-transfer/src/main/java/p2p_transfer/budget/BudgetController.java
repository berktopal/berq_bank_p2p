package p2p_transfer.budget;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import p2p_transfer.account.AccountDtos.SupportedCurrency;
import p2p_transfer.auth.AuthUser;
import p2p_transfer.budget.BudgetService.BudgetResponse;
import p2p_transfer.transfer.TransactionCategory;

import java.math.BigDecimal;
import java.util.List;

@Tag(name = "Budgets")
@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    private final BudgetService service;

    public BudgetController(BudgetService service) {
        this.service = service;
    }

    public record UpsertBudgetRequest(
            @NotNull TransactionCategory category,
            @NotNull SupportedCurrency currency,
            @NotNull @DecimalMin(value = "1.00", message = "Bütçe en az 1 olmalıdır.")
            @Digits(integer = 15, fraction = 2) BigDecimal monthlyLimit) {
    }

    @Operation(summary = "Bu ayki harcamalarıyla birlikte bütçeler")
    @GetMapping
    public List<BudgetResponse> list(@AuthenticationPrincipal AuthUser me) {
        return service.list(me.id());
    }

    @Operation(summary = "Bütçe oluştur ya da limitini güncelle (kategori + para birimi başına bir tane)")
    @PutMapping
    public BudgetResponse upsert(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody UpsertBudgetRequest request) {
        return service.upsert(me.id(), request.category(), request.currency().name(), request.monthlyLimit());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        service.delete(me.id(), id);
    }
}
