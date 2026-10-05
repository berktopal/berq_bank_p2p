package p2p_transfer.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.ZoneId;

/** Uygulamaya özgü, ortama göre değişebilen iş kuralı ayarları ({@code app.*}). */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(Bank bank, Transfer transfer, Security security, Onboarding onboarding) {

    public record Bank(@Pattern(regexp = "\\d{5}") String code, @NotNull ZoneId zone, @Min(1) int maxAccountsPerUser) {
    }

    /** Hesap başına, banka saat dilimine göre takvim günü bazında giden transfer limiti. */
    public record Transfer(@NotNull BigDecimal dailyLimit) {
    }

    public record Security(@Min(1) int maxFailedLogins, @NotNull Duration lockDuration) {
    }

    /** Yeni kayıtta açılan hesabın başlangıç bakiyesi (yalnızca demo ortamında 0'dan büyük olmalı). */
    public record Onboarding(@NotNull BigDecimal welcomeBalance) {
    }
}
