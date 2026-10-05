package p2p_transfer.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import p2p_transfer.auth.AuthUser;
import p2p_transfer.common.error.ErrorCode;
import p2p_transfer.common.error.ProblemResponses;
import p2p_transfer.config.FeatureProperties.RateLimit;
import p2p_transfer.config.FeatureProperties.RateLimit.Rule;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Kötüye kullanıma açık uç noktalarda istek sınırı. Spring Security zincirinin sonunda çalışır,
 * böylece kimliği doğrulanmış isteklerde kullanıcı bazında sayılabilir.
 * <ul>
 *   <li>giriş / kayıt: IP başına (kaba kuvvet ve toplu hesap açma)</li>
 *   <li>IBAN'dan isim öğrenilebilen her şey (sorgu, alıcı kaydetme, talimat): kullanıcı başına ortak kota</li>
 *   <li>transfer, istek ödeme, para isteme: kullanıcı başına</li>
 * </ul>
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private record Route(String name, String method, Pattern path, boolean perUser, Function<RateLimit, Rule> rule) {
    }

    private static final List<Route> ROUTES = List.of(
            new Route("login", "POST", Pattern.compile("^/api/auth/login$"), false, RateLimit::login),
            new Route("register", "POST", Pattern.compile("^/api/auth/register$"), false, RateLimit::register),
            new Route("lookup", "GET", Pattern.compile("^/api/accounts/lookup$"), true, RateLimit::lookup),
            // IBAN'dan isim döndüren diğer uç noktalar da aynı kotayı paylaşır; yoksa tarama koruması bunlarla aşılırdı
            new Route("lookup", "POST", Pattern.compile("^/api/(contacts|scheduled-transfers)$"), true, RateLimit::lookup),
            new Route("transfer", "POST", Pattern.compile("^/api/(transactions/transfer|payment-requests/\\d+/pay)$"), true, RateLimit::transfer),
            new Route("payment-request", "POST", Pattern.compile("^/api/payment-requests$"), true, RateLimit::paymentRequest));

    private final RateLimiter limiter;
    private final RateLimit config;

    public RateLimitFilter(RateLimiter limiter, RateLimit config) {
        this.limiter = limiter;
        this.config = config;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !config.enabled() || !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        for (Route route : ROUTES) {
            if (!route.method().equals(request.getMethod()) || !route.path().matcher(path).matches()) {
                continue;
            }
            String subject = route.perUser() ? currentUserId() : request.getRemoteAddr();
            if (subject != null) {
                RateLimiter.Decision decision = limiter.tryConsume(route.name() + ":" + subject, route.rule().apply(config));
                if (!decision.allowed()) {
                    response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(decision.retryAfterSeconds()));
                    ProblemResponses.write(response, ErrorCode.RATE_LIMITED,
                            Map.of("retryAfterSeconds", decision.retryAfterSeconds()));
                    return;
                }
            }
            break;
        }
        chain.doFilter(request, response);
    }

    private static String currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof AuthUser user ? String.valueOf(user.id()) : null;
    }
}
