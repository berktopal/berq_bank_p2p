package p2p_transfer.schedule;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import p2p_transfer.auth.AuthUser;
import p2p_transfer.schedule.ScheduledTransferDtos.CreateScheduleRequest;
import p2p_transfer.schedule.ScheduledTransferDtos.ScheduledTransferResponse;
import p2p_transfer.schedule.ScheduledTransferDtos.UpdateScheduleRequest;

import java.util.List;

@Tag(name = "Scheduled transfers")
@RestController
@RequestMapping("/api/scheduled-transfers")
public class ScheduledTransferController {

    private final ScheduledTransferService service;

    public ScheduledTransferController(ScheduledTransferService service) {
        this.service = service;
    }

    @GetMapping
    public List<ScheduledTransferResponse> list(@AuthenticationPrincipal AuthUser me) {
        return service.list(me.id());
    }

    @Operation(summary = "İleri tarihli (ONCE) veya düzenli (WEEKLY / MONTHLY) transfer talimatı oluştur")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ScheduledTransferResponse create(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody CreateScheduleRequest body) {
        return service.create(me.id(), body);
    }

    @Operation(summary = "Duraklat (PAUSED) veya sürdür (ACTIVE)")
    @PatchMapping("/{id}")
    public ScheduledTransferResponse update(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                                            @Valid @RequestBody UpdateScheduleRequest body) {
        return service.updateStatus(me.id(), id, body.status());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        service.cancel(me.id(), id);
    }
}
