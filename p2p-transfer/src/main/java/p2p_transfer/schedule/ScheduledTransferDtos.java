package p2p_transfer.schedule;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import p2p_transfer.account.Account;
import p2p_transfer.common.Masking;
import p2p_transfer.schedule.ScheduledTransfer.Frequency;
import p2p_transfer.schedule.ScheduledTransfer.Status;
import p2p_transfer.transfer.TransactionCategory;
import p2p_transfer.user.User;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public final class ScheduledTransferDtos {

    private ScheduledTransferDtos() {
    }

    public record CreateScheduleRequest(
            @NotNull Long fromAccountId,
            @NotBlank String toIban,
            @NotNull @DecimalMin(value = "0.01", message = "Tutar en az 0,01 olmalıdır.")
            @Digits(integer = 15, fraction = 2, message = "Tutar en fazla 2 ondalık basamak içerebilir.") BigDecimal amount,
            @Size(max = 140) String description,
            TransactionCategory category,
            @NotNull Frequency frequency,
            @NotNull LocalDate startDate,
            LocalDate endDate) {
    }

    /** Yalnızca ACTIVE ↔ PAUSED geçişi; iptal DELETE ile yapılır. */
    public record UpdateScheduleRequest(@NotNull Status status) {
    }

    public record AccountRef(Long id, String iban, String name) {
    }

    public record ScheduledTransferResponse(Long id, AccountRef from, String toIban, String toName, BigDecimal amount,
                                            String currency, String description, TransactionCategory category,
                                            Frequency frequency, int dayOfMonth, LocalDate nextRunDate, LocalDate endDate,
                                            Status status, Instant lastRunAt, String lastError, int runCount,
                                            Instant createdAt) {

        public static ScheduledTransferResponse of(ScheduledTransfer s) {
            Account from = s.getFromAccount();
            Account to = s.getToAccount();
            // Kendi hesabına talimatta hesap adı; başkasına talimatta IBAN sorgusundaki gibi maskeli isim.
            // (Tam isim dönseydi, talimat oluşturup iptal ederek herhangi bir IBAN'ın sahibi öğrenilebilirdi.)
            User owner = to.getUser();
            String toName = owner.getId().equals(s.getUserId())
                    ? to.getName()
                    : Masking.maskedName(owner.getFirstName(), owner.getLastName());
            return new ScheduledTransferResponse(s.getId(), new AccountRef(from.getId(), from.getIban(), from.getName()),
                    to.getIban(), toName, s.getAmount(), from.getCurrency(), s.getDescription(), s.getCategory(),
                    s.getFrequency(), s.getDayOfMonth(), s.getNextRunDate(), s.getEndDate(), s.getStatus(), s.getLastRunAt(),
                    s.getLastError(), s.getRunCount(), s.getCreatedAt());
        }
    }
}
