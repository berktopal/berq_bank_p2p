package p2p_transfer.schedule;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;

/** Vadesi gelen talimatları periyodik olarak çalıştırır (aralık: app.scheduled-transfers.poll-interval). */
@Slf4j
@Component
public class ScheduledTransferJob {

    private final ScheduledTransferService service;
    private final Clock clock;

    public ScheduledTransferJob(ScheduledTransferService service, Clock clock) {
        this.service = service;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${app.scheduled-transfers.poll-interval:60s}", initialDelayString = "10s")
    public void run() {
        int processed = service.runDue(clock.instant());
        if (processed > 0) {
            log.info("Processed {} scheduled transfer(s)", processed);
        }
    }
}
