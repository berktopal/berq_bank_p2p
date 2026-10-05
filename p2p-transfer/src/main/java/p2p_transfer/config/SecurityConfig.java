package p2p_transfer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import p2p_transfer.common.error.ErrorCode;
import p2p_transfer.common.error.ProblemResponses;
import p2p_transfer.ratelimit.RateLimitFilter;
import p2p_transfer.ratelimit.RateLimiter;


/**
 * Sunucu taraflı oturum + SPA uyumlu CSRF koruması.
 * <ul>
 *   <li>Oturum çerezi HttpOnly + SameSite=Strict (application.properties)</li>
 *   <li>CSRF token'ı okunabilir {@code XSRF-TOKEN} çerezinde; istemci {@code X-XSRF-TOKEN} başlığıyla geri yollar</li>
 *   <li>Yetkisiz API çağrıları HTML yönlendirmesi değil, ProblemDetail JSON alır</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String CSP = String.join("; ",
            "default-src 'self'",
            "script-src 'self'",
            "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com",
            "font-src 'self' https://fonts.gstatic.com",
            "img-src 'self' data:",
            "connect-src 'self'",
            "frame-ancestors 'none'",
            "base-uri 'self'",
            "form-action 'self'");

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, RateLimiter rateLimiter,
                                            FeatureProperties.RateLimit rateLimit) throws Exception {
        http
                .csrf(csrf -> csrf.spa())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/register").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/csrf", "/api/public/**").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                .securityContext(sc -> sc.securityContextRepository(securityContextRepository()))
                .sessionManagement(sm -> sm.sessionFixation(sf -> sf.changeSessionId()))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> ProblemResponses.write(res, ErrorCode.UNAUTHENTICATED))
                        .accessDeniedHandler((req, res, e) -> ProblemResponses.write(res, ErrorCode.ACCESS_DENIED)))
                // Bean olarak değil burada oluşturulur: aksi halde servlet konteyneri de ayrıca kaydeder, iki kez çalışır
                .addFilterAfter(new RateLimitFilter(rateLimiter, rateLimit), AuthorizationFilter.class)
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .deleteCookies("BQSESSION")
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(CSP))
                        .referrerPolicy(rp -> rp.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable);
        return http.build();
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
