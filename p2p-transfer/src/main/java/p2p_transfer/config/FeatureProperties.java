package p2p_transfer.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/** v3 özelliklerinin ayarları: hız sınırları ve para isteklerinin geçerlilik süresi. */
public final class FeatureProperties {

    private FeatureProperties() {
    }

    /**
     * Token bucket: {@code capacity} kadar ani istek, ardından {@code period} başına {@code capacity} istek.
     * Giriş/kayıt IP'ye, diğerleri oturumdaki kullanıcıya göre sayılır.
     */
    @Validated
    @ConfigurationProperties(prefix = "app.rate-limit")
    public record RateLimit(boolean enabled, @Valid @NotNull Rule login, @Valid @NotNull Rule register,
                            @Valid @NotNull Rule lookup, @Valid @NotNull Rule transfer,
                            @Valid @NotNull Rule paymentRequest) {

        public record Rule(@Min(1) int capacity, @NotNull Duration period) {
        }
    }

    @Validated
    @ConfigurationProperties(prefix = "app.payment-requests")
    public record PaymentRequests(@NotNull Duration expiry) {
    }
}
