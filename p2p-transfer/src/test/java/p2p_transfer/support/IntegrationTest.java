package p2p_transfer.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import p2p_transfer.ratelimit.RateLimiter;

/**
 * Tüm entegrasyon testlerinin tabanı: gerçek PostgreSQL + Flyway migration'ları + tam Spring Security zinciri.
 * Her test temiz bir veritabanıyla başlar.
 */
@SpringBootTest
@Import(TestData.class)
@AutoConfigureMockMvc
public abstract class IntegrationTest {

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected TestData data;

    @Autowired
    protected RateLimiter rateLimiter;

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> EmbeddedPostgres.jdbcUrl("postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "30");
        // Gerçek derlenmiş SPA yerine küçük bir sahte kabuk (src/test/resources/spa-test)
        registry.add("spring.web.resources.static-locations", () -> "classpath:/spa-test/");
        // Zamanlanmış görevler testin verisini arka planda değiştirmesin; testler işleri doğrudan çağırır
        registry.add("app.scheduling.enabled", () -> "false");
    }

    @BeforeEach
    void cleanDatabase() {
        jdbc.execute("TRUNCATE notifications, budgets, scheduled_transfers, payment_requests, contacts, transactions, "
                + "accounts, users RESTART IDENTITY CASCADE");
        rateLimiter.reset();
    }
}
