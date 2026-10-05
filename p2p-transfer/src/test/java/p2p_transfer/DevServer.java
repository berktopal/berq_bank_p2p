package p2p_transfer;

import org.springframework.boot.SpringApplication;
import p2p_transfer.support.EmbeddedPostgres;

/**
 * Kurulumsuz yerel çalıştırma: gömülü PostgreSQL + demo verisi.
 * <pre>./mvnw spring-boot:test-run</pre>
 * Veritabanı her başlatmada sıfırdan oluşturulur; kalıcı veri için normal {@code spring-boot:run} kullanın.
 * Uçtan uca (Playwright) testler de bu sunucuya karşı koşar.
 */
public final class DevServer {

    private DevServer() {
    }

    public static void main(String[] args) {
        System.setProperty("spring.datasource.url", EmbeddedPostgres.jdbcUrl("postgres"));
        System.setProperty("spring.datasource.username", "postgres");
        System.setProperty("spring.datasource.password", "");
        // Uçtan uca testler aynı IP'den dakikada onlarca kez giriş/kayıt yapar; üretim limitleri burada gevşetilir.
        // (Limitlerin kendisi RateLimitApiTests ile doğrulanır.)
        System.setProperty("app.rate-limit.login.capacity", "200");
        System.setProperty("app.rate-limit.register.capacity", "100");
        SpringApplication.from(P2pTransferApplication::main)
                .withAdditionalProfiles("demo")
                .run(args);
    }
}
