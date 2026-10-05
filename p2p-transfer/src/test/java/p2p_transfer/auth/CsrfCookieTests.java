package p2p_transfer.auth;

import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import p2p_transfer.support.IntegrationTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SPA'nın CSRF sözleşmesi: token okunabilir bir çerezde gelir, X-XSRF-TOKEN başlığıyla geri gönderilir.
 * Ayrı ve taze bir context gerekir: spring-security-test'in {@code csrf()} post-processor'ü, paylaşılan
 * CsrfFilter'ın token deposunu kalıcı olarak test sarmalayıcısıyla değiştirir.
 */
@DirtiesContext(classMode = ClassMode.BEFORE_CLASS)
class CsrfCookieTests extends IntegrationTest {

    @Test
    void csrfEndpointIssuesReadableCookieForTheSpa() throws Exception {
        mvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"))
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andExpect(cookie().httpOnly("XSRF-TOKEN", false));
    }
}
