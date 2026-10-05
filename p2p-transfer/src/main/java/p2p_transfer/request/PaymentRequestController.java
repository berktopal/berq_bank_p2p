package p2p_transfer.request;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import p2p_transfer.auth.AuthUser;
import p2p_transfer.common.PageResponse;
import p2p_transfer.request.PaymentRequestDtos.CreatePaymentRequest;
import p2p_transfer.request.PaymentRequestDtos.PayRequest;
import p2p_transfer.request.PaymentRequestDtos.PaymentRequestResponse;
import p2p_transfer.request.PaymentRequestDtos.Role;

import java.util.Map;

@Tag(name = "Payment requests")
@RestController
@RequestMapping("/api/payment-requests")
public class PaymentRequestController {

    private final PaymentRequestService service;

    public PaymentRequestController(PaymentRequestService service) {
        this.service = service;
    }

    @Operation(summary = "Para iste: ödeyen IBAN ile belirtilir, para seçilen kendi hesabına yatar")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentRequestResponse create(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody CreatePaymentRequest body) {
        return service.create(me.id(), body);
    }

    @GetMapping
    public PageResponse<PaymentRequestResponse> list(@AuthenticationPrincipal AuthUser me,
                                                     @RequestParam(required = false) Role role,
                                                     @RequestParam(required = false) PaymentRequest.Status status,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "20") int size) {
        return service.list(me.id(), role, status, page, size);
    }

    @GetMapping("/pending-count")
    public Map<String, Long> pendingCount(@AuthenticationPrincipal AuthUser me) {
        return Map.of("count", service.pendingIncomingCount(me.id()));
    }

    @Operation(summary = "Gelen isteği öde (aynı istek iki kez ödenemez)")
    @PostMapping("/{id}/pay")
    public PaymentRequestResponse pay(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                                      @Valid @RequestBody PayRequest body) {
        return service.pay(me.id(), id, body.fromAccountId());
    }

    @PostMapping("/{id}/decline")
    public PaymentRequestResponse decline(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return service.decline(me.id(), id);
    }

    @PostMapping("/{id}/cancel")
    public PaymentRequestResponse cancel(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return service.cancel(me.id(), id);
    }
}
