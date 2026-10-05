package p2p_transfer.schedule;

import org.junit.jupiter.api.Test;
import p2p_transfer.schedule.ScheduledTransfer.Frequency;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class OccurrenceTest {

    @Test
    void monthlyScheduleKeepsItsTargetDayAcrossShortMonths() {
        ScheduledTransfer s = schedule(Frequency.MONTHLY, LocalDate.of(2026, 1, 31));

        LocalDate feb = s.occurrenceAfter(LocalDate.of(2026, 1, 31));
        assertThat(feb).isEqualTo(LocalDate.of(2026, 2, 28));

        s.setNextRunDate(feb);
        assertThat(s.occurrenceAfter(feb)).isEqualTo(LocalDate.of(2026, 3, 31));
    }

    @Test
    void leapYearFebruaryUsesThe29th() {
        ScheduledTransfer s = schedule(Frequency.MONTHLY, LocalDate.of(2028, 1, 30));
        assertThat(s.occurrenceAfter(LocalDate.of(2028, 1, 30))).isEqualTo(LocalDate.of(2028, 2, 29));
    }

    @Test
    void skipsMissedPeriodsInsteadOfReturningPastDates() {
        ScheduledTransfer s = schedule(Frequency.WEEKLY, LocalDate.of(2026, 3, 2));
        assertThat(s.occurrenceAfter(LocalDate.of(2026, 3, 25))).isEqualTo(LocalDate.of(2026, 3, 30));
    }

    private static ScheduledTransfer schedule(Frequency frequency, LocalDate start) {
        ScheduledTransfer s = new ScheduledTransfer();
        s.setFrequency(frequency);
        s.setDayOfMonth(start.getDayOfMonth());
        s.setNextRunDate(start);
        return s;
    }
}
