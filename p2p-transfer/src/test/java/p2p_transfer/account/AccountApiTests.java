package p2p_transfer.account;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import p2p_transfer.common.Iban;
import p2p_transfer.support.IntegrationTest;
import p2p_transfer.user.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static p2p_transfer.support.Money.eq;
import static p2p_transfer.support.TestData.as;

class AccountApiTests extends IntegrationTest {

    @Test
    void openedAccountGetsServerGeneratedValidIbanAndZeroBalance() throws Exception {
        User ada = data.user("Ada");
        String body = mvc.perform(post("/api/accounts").with(as(ada)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        // istemcinin gönderdiği bakiye/IBAN yok sayılmalı
                        .content("{\"name\":\"Tatil\",\"currency\":\"EUR\",\"balance\":1000000,\"iban\":\"TR000000000000000000000001\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Tatil"))
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.balance", eq("0")))
                .andReturn().getResponse().getContentAsString();

        String iban = JsonPath.read(body, "$.iban");
        assertThat(iban).isNotEqualTo("TR000000000000000000000001");
        assertThat(Iban.hasValidChecksum(iban)).isTrue();
    }

    @Test
    void accountCountPerUserIsLimited() throws Exception {
        User ada = data.user("Ada");
        for (int i = 0; i < 5; i++) {
            data.account(ada, "0");
        }
        mvc.perform(post("/api/accounts").with(as(ada)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Fazla\",\"currency\":\"TRY\"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("ACCOUNT_LIMIT_REACHED"))
                .andExpect(jsonPath("$.max").value(5));
    }

    @Test
    void unsupportedCurrencyIsRejected() throws Exception {
        mvc.perform(post("/api/accounts").with(as(data.user("Ada"))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"X\",\"currency\":\"BTC\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void usersCannotSeeOrRenameOthersAccounts() throws Exception {
        User ada = data.user("Ada");
        User eve = data.user("Eve");
        Account adaAcc = data.account(ada, "100");

        mvc.perform(get("/api/accounts/{id}", adaAcc.getId()).with(as(eve)))
                .andExpect(status().isNotFound());
        mvc.perform(patch("/api/accounts/{id}", adaAcc.getId()).with(as(eve)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"hacked\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/accounts").with(as(eve)))
                .andExpect(jsonPath("$.length()").value(0));

        mvc.perform(patch("/api/accounts/{id}", adaAcc.getId()).with(as(ada)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Maaş\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Maaş"));
    }

    @Test
    void ibanLookupRevealsOnlyMaskedNameAndCurrency() throws Exception {
        User ada = data.user("Ada");
        Account bobAcc = data.account(data.user("Bob"), "777");

        mvc.perform(get("/api/accounts/lookup").param("iban", Iban.format(bobAcc.getIban())).with(as(ada)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerName").value("Bob T***"))
                .andExpect(jsonPath("$.currency").value("TRY"))
                .andExpect(jsonPath("$.ownAccount").value(false))
                .andExpect(jsonPath("$.balance").doesNotExist())
                .andExpect(jsonPath("$.id").doesNotExist());

        mvc.perform(get("/api/accounts/lookup").param("iban", "TR12").with(as(ada)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.iban").exists());
    }
}
