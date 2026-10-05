package p2p_transfer.analytics;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import p2p_transfer.analytics.AnalyticsService.Summary;
import p2p_transfer.auth.AuthUser;

@Tag(name = "Analytics")
@Validated
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/summary")
    public Summary summary(@AuthenticationPrincipal AuthUser me,
                           @RequestParam Long accountId,
                           @RequestParam(defaultValue = "6") @Min(1) @Max(12) int months) {
        return analyticsService.summary(me.id(), accountId, months);
    }
}
