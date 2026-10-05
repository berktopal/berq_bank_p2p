package p2p_transfer.schedule;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import p2p_transfer.account.Account;
import p2p_transfer.support.IntegrationTest;
import p2p_transfer.transfer.TransactionCategory;
import p2p_transfer.transfer.TransferCompleted;
import p2p_transfer.transfer.TransferDtos.TransferRequest;
import p2p_transfer.transfer.TransferService;
import p2p_transfer.user.User;

import java.math.BigDecimal;
import java.util.List;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static p2p_transfer.support.TestData.as;

class ScheduledTransferTests extends IntegrationTest {

    private static final ZoneId ISTANBUL = ZoneId.of("Europe/Istanbul");

    @Autowired
    ScheduledTransferService service;

    @Autowired
    TransferService transferService;

    User ada;
    Account adaAcc;
    Account bobAcc;
    LocalDate today;

    @BeforeEach
    void setUp() {
        ada = data.user("Ada");
        adaAcc = data.account(ada, "1000");
        bobAcc = data.account(data.user("Bob"), "0");
        today = LocalDate.now(ISTANBUL);
    }

    @Test
    void oneOffTransferRunsOnItsDateAndCompletes() throws Exception {
        Integer id = idOf(create("ONCE", today.plusDays(3), null, "250").andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.toName").value("Bob Test")));

        assertThat(service.runDue(at(today.plusDays(2)))).isZero();
        assertThat(data.balance(bobAcc)).isEqualByComparingTo("0");

        assertThat(service.runDue(at(today.plusDays(3)))).isEqualTo(1);
        assertThat(data.balance(bobAcc)).isEqualByComparingTo("250");

        mvc.perform(get("/api/scheduled-transfers").with(as(ada)))
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$[0].runCount").value(1));
        assertThat(types(ada)).containsExactly("SCHEDULED_TRANSFER_EXECUTED");
        assertThat(types(bobAcc.getUser())).containsExactly("MONEY_RECEIVED");
    }

    @Test
    void runningTheJobTwiceOnTheSameDayPaysOnce() throws Exception {
        create("MONTHLY", today, null, "100");
        service.runDue(at(today));
        service.runDue(at(today));
        assertThat(data.balance(bobAcc)).isEqualByComparingTo("100");
        assertThat(jdbc.queryForObject("select count(*) from transactions", Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select next_run_date from scheduled_transfers", LocalDate.class))
                .isEqualTo(today.plusMonths(1).withDayOfMonth(Math.min(today.getDayOfMonth(), today.plusMonths(1).lengthOfMonth())));
    }

    @Test
    void crashAfterTransferCommitDoesNotPayTwice() throws Exception {
        Integer id = idOf(create("WEEKLY", today, null, "40"));
        // Önceki tur transferi commit etti ama talimatı ilerletemeden çöktü
        transferService.transfer(ada.getId(), new TransferRequest(adaAcc.getId(), bobAcc.getIban(), new BigDecimal("40"),
                null, TransactionCategory.GENERAL), "sched-" + id + "-" + today, TransferCompleted.Origin.SCHEDULED);

        service.runDue(at(today));
        assertThat(data.balance(bobAcc)).isEqualByComparingTo("40");
        assertThat(jdbc.queryForObject("select next_run_date from scheduled_transfers", LocalDate.class))
                .isEqualTo(today.plusWeeks(1));
    }

    @Test
    void failedRunIsReportedAndRetriedNextPeriodNotEveryMinute() throws Exception {
        create("WEEKLY", today, null, "5000");
        service.runDue(at(today));
        service.runDue(at(today));

        assertThat(data.balance(adaAcc)).isEqualByComparingTo("1000");
        assertThat(types(ada)).containsExactly("SCHEDULED_TRANSFER_FAILED");
        mvc.perform(get("/api/scheduled-transfers").with(as(ada)))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[0].lastError").value("INSUFFICIENT_FUNDS"))
                .andExpect(jsonPath("$[0].nextRunDate").value(today.plusWeeks(1).toString()));
    }

    @Test
    void pausedSchedulesDoNotRunAndResumingSkipsMissedDates() throws Exception {
        Integer id = idOf(create("WEEKLY", today, null, "10"));
        mvc.perform(patch("/api/scheduled-transfers/{id}", id).with(as(ada)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"PAUSED\"}"))
                .andExpect(jsonPath("$.status").value("PAUSED"));
        assertThat(service.runDue(at(today.plusWeeks(3)))).isZero();

        // Geçmişte kalmış bir sonraki tarih: sürdürünce kaçırılan haftalar toplu gönderilmez
        jdbc.update("update scheduled_transfers set next_run_date = ? where id = ?", today.minusWeeks(2), id);
        mvc.perform(patch("/api/scheduled-transfers/{id}", id).with(as(ada)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACTIVE\"}"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.nextRunDate").value(today.toString()));
    }

    @Test
    void endDateFinishesRecurringSchedule() throws Exception {
        create("WEEKLY", today, today.plusDays(10), "10");
        service.runDue(at(today));
        service.runDue(at(today.plusWeeks(1)));
        assertThat(jdbc.queryForObject("select status from scheduled_transfers", String.class)).isEqualTo("COMPLETED");
        assertThat(data.balance(bobAcc)).isEqualByComparingTo("20");
    }

    @Test
    void validatesDatesCurrencyAndOwnership() throws Exception {
        create("ONCE", today.minusDays(1), null, "10")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("SCHEDULE_INVALID_DATES"));
        create("MONTHLY", today.plusDays(5), today.plusDays(1), "10")
                .andExpect(jsonPath("$.code").value("SCHEDULE_INVALID_DATES"));

        Account usd = data.account(bobAcc.getUser(), "0", "USD");
        mvc.perform(post("/api/scheduled-transfers").with(as(ada)).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(body(adaAcc.getId(), usd.getIban(), "ONCE", today, null, "10")))
                .andExpect(jsonPath("$.code").value("CURRENCY_MISMATCH"));

        Integer id = idOf(create("ONCE", today.plusDays(1), null, "10"));
        mvc.perform(delete("/api/scheduled-transfers/{id}", id).with(as(data.user("Eve"))).with(csrf()))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/scheduled-transfers/{id}", id).with(as(ada)).with(csrf()))
                .andExpect(status().isNoContent());
        assertThat(service.runDue(at(today.plusDays(1)))).isZero();
    }

    private ResultActions create(String frequency, LocalDate start, LocalDate end, String amount) throws Exception {
        return mvc.perform(post("/api/scheduled-transfers").with(as(ada)).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(body(adaAcc.getId(), bobAcc.getIban(), frequency, start, end, amount)));
    }

    private static String body(Long from, String toIban, String frequency, LocalDate start, LocalDate end, String amount) {
        return """
                {"fromAccountId":%d,"toIban":"%s","amount":%s,"frequency":"%s","startDate":"%s"%s,"category":"RENT"}"""
                .formatted(from, toIban, amount, frequency, start, end == null ? "" : ",\"endDate\":\"" + end + "\"");
    }

    private static Instant at(LocalDate day) {
        return day.atTime(9, 0).atZone(ISTANBUL).toInstant();
    }

    private static Integer idOf(ResultActions actions) throws Exception {
        return JsonPath.read(actions.andReturn().getResponse().getContentAsString(), "$.id");
    }

    private List<String> types(User user) {
        return jdbc.queryForList("select type from notifications where user_id = ? order by id", String.class, user.getId());
    }
}
