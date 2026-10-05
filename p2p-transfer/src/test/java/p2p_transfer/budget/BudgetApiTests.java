package p2p_transfer.budget;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import p2p_transfer.account.Account;
import p2p_transfer.support.IntegrationTest;
import p2p_transfer.transfer.TransactionCategory;
import p2p_transfer.transfer.TransferDtos.TransferRequest;
import p2p_transfer.transfer.TransferService;
import p2p_transfer.user.User;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static p2p_transfer.support.Money.eq;
import static p2p_transfer.support.TestData.as;

class BudgetApiTests extends IntegrationTest {

    @Autowired
    TransferService transferService;

    User ada;
    Account adaAcc;
    Account bobAcc;

    @BeforeEach
    void setUp() {
        ada = data.user("Ada");
        adaAcc = data.account(ada, "10000");
        bobAcc = data.account(data.user("Bob"), "0");
    }

    @Test
    void spendingCountsOnlyTransfersToOtherPeopleInTheBudgetCurrency() throws Exception {
        upsert(ada, "FOOD", "TRY", "1000").andExpect(status().isOk());

        send(adaAcc, bobAcc, "300", TransactionCategory.FOOD);
        send(adaAcc, bobAcc, "50", TransactionCategory.RENT);                      // başka kategori
        send(adaAcc, data.account(ada, "0"), "999", TransactionCategory.FOOD);    // kendi hesabına virman

        mvc.perform(get("/api/budgets").with(as(ada)))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].spent", eq("300")))
                .andExpect(jsonPath("$[0].remaining", eq("700")))
                .andExpect(jsonPath("$[0].percent").value(30))
                .andExpect(jsonPath("$[0].status").value("OK"));
    }

    @Test
    void upsertUpdatesTheExistingBudgetInsteadOfDuplicating() throws Exception {
        upsert(ada, "FOOD", "TRY", "1000");
        upsert(ada, "FOOD", "TRY", "1500").andExpect(jsonPath("$.monthlyLimit", eq("1500")));
        mvc.perform(get("/api/budgets").with(as(ada))).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void warnsOnceWhenCrossing80PercentAndOnceWhenExceeded() throws Exception {
        upsert(ada, "FOOD", "TRY", "1000");

        send(adaAcc, bobAcc, "700", TransactionCategory.FOOD);   // %70: sessiz
        assertThat(notificationTypes()).isEmpty();

        send(adaAcc, bobAcc, "150", TransactionCategory.FOOD);   // %85: uyarı
        send(adaAcc, bobAcc, "50", TransactionCategory.FOOD);    // %90: tekrar uyarı yok
        assertThat(notificationTypes()).containsExactly("BUDGET_WARNING");

        send(adaAcc, bobAcc, "200", TransactionCategory.FOOD);   // %110: aşıldı
        send(adaAcc, bobAcc, "10", TransactionCategory.FOOD);    // hâlâ aşılmış: tekrar yok
        assertThat(notificationTypes()).containsExactly("BUDGET_WARNING", "BUDGET_EXCEEDED");

        mvc.perform(get("/api/budgets").with(as(ada)))
                .andExpect(jsonPath("$[0].status").value("EXCEEDED"))
                .andExpect(jsonPath("$[0].remaining", eq("0")));
    }

    @Test
    void validationAndOwnership() throws Exception {
        upsert(ada, "FOOD", "TRY", "0")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.monthlyLimit").exists());

        Integer id = JsonPath.read(upsert(ada, "FOOD", "TRY", "100").andReturn().getResponse().getContentAsString(), "$.id");
        mvc.perform(delete("/api/budgets/{id}", id).with(as(data.user("Eve"))).with(csrf()))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/budgets/{id}", id).with(as(ada)).with(csrf()))
                .andExpect(status().isNoContent());
    }

    private ResultActions upsert(User user, String category, String currency, String limit) throws Exception {
        return mvc.perform(put("/api/budgets").with(as(user)).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"category":"%s","currency":"%s","monthlyLimit":%s}""".formatted(category, currency, limit)));
    }

    private void send(Account from, Account to, String amount, TransactionCategory category) {
        transferService.transfer(ada.getId(), new TransferRequest(from.getId(), to.getIban(), new BigDecimal(amount), null, category), null);
    }

    private List<String> notificationTypes() {
        return jdbc.queryForList("select type from notifications where user_id = ? order by id", String.class, ada.getId());
    }
}
