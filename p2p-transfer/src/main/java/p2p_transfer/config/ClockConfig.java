package p2p_transfer.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties({AppProperties.class, FeatureProperties.RateLimit.class, FeatureProperties.PaymentRequests.class})
public class ClockConfig {

    /** Zaman servislere enjekte edilir; testlerde sabit saatle değiştirilebilir. */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
