package p2p_transfer.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI berqBankOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Berq Bank API")
                .version("v2")
                .description("""
                        P2P para transferi API'si. Kimlik doğrulama oturum çereziyle (BQSESSION) yapılır; \
                        durum değiştiren her istek X-XSRF-TOKEN başlığı gerektirir (GET /api/auth/csrf). \
                        Hatalar RFC 9457 Problem Details formatında ve makine-okunur bir `code` alanıyla döner."""));
    }
}
