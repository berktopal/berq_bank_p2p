package p2p_transfer.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import p2p_transfer.common.Masking;
import p2p_transfer.user.User;

import java.math.BigDecimal;
import java.time.Instant;

public final class AccountDtos {

    private AccountDtos() {
    }

    public enum SupportedCurrency { TRY, USD, EUR }

    public record OpenAccountRequest(
            @NotBlank @Size(max = 60) String name,
            @NotNull SupportedCurrency currency) {
    }

    public record RenameAccountRequest(@NotBlank @Size(max = 60) String name) {
    }

    /** Hesap sahibine gösterilen tam görünüm (bakiye dahil). */
    public record AccountResponse(Long id, String iban, String name, BigDecimal balance, String currency, Instant createdAt) {
        public static AccountResponse of(Account a) {
            return new AccountResponse(a.getId(), a.getIban(), a.getName(), a.getBalance(), a.getCurrency(), a.getCreatedAt());
        }
    }

    /** IBAN sorgusu: yalnızca transferi onaylamaya yetecek kadar bilgi (maskeli isim, para birimi). */
    public record AccountLookupResponse(String iban, String ownerName, String currency, boolean ownAccount) {
        public static AccountLookupResponse of(Account a, Long currentUserId) {
            User u = a.getUser();
            return new AccountLookupResponse(a.getIban(), Masking.maskedName(u.getFirstName(), u.getLastName()),
                    a.getCurrency(), a.isOwnedBy(currentUserId));
        }
    }
}
