package p2p_transfer.config;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import p2p_transfer.demo.DemoDataSeeder;

/** Giriş yapmadan okunabilen arayüz ayarları. Demo kimlik bilgileri yalnızca demo profilinde döner. */
@Tag(name = "Public")
@RestController
@RequestMapping("/api/public")
public class PublicConfigController {

    private final boolean demo;

    public PublicConfigController(@Value("${app.demo.seed:false}") boolean demo) {
        this.demo = demo;
    }

    public record PublicConfig(boolean demo, String demoEmail, String demoPassword) {
    }

    @GetMapping("/config")
    public PublicConfig config() {
        return demo
                ? new PublicConfig(true, DemoDataSeeder.DEMO_EMAIL, DemoDataSeeder.DEMO_PASSWORD)
                : new PublicConfig(false, null, null);
    }
}
