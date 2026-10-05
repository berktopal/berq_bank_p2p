package p2p_transfer.ratelimit;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import p2p_transfer.account.Account;
import p2p_transfer.support.IntegrationTest;
import p2p_transfer.user.User;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static p2p_transfer.support.TestData.as;

class RateLimitApiTests extends IntegrationTest {

    @Test
    void loginIsLimitedPerClientIpEvenAcrossDifferentEmails() throws Exception {
        for (int i = 0; i < 10; i++) {
            login("nobody" + i + "@test.local").andExpect(status().isUnauthorized());
        }
        login("someone-else@test.local")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"))
                .andExpect(jsonPath("$.retryAfterSeconds").isNumber());
    }

    @Test
    void ibanLookupIsLimitedPerUserSoIbansCannotBeEnumerated() throws Exception {
        User eve = data.user("Eve");
        User ada = data.user("Ada");
        Account target = data.account(data.user("Bob"), "0");

        for (int i = 0; i < 30; i++) {
            mvc.perform(get("/api/accounts/lookup").param("iban", target.getIban()).with(as(eve)))
                    .andExpect(status().isOk());
        }
        mvc.perform(get("/api/accounts/lookup").param("iban", target.getIban()).with(as(eve)))
                .andExpect(status().isTooManyRequests());
        // Başka bir kullanıcının kotası etkilenmez
        mvc.perform(get("/api/accounts/lookup").param("iban", target.getIban()).with(as(ada)))
                .andExpect(status().isOk());
    }

    @Test
    void savingContactsAndCreatingSchedulesShareTheLookupQuota() throws Exception {
        User eve = data.user("Eve");
        Account target = data.account(data.user("Bob"), "0");
        for (int i = 0; i < 30; i++) {
            mvc.perform(get("/api/accounts/lookup").param("iban", target.getIban()).with(as(eve)))
                    .andExpect(status().isOk());
        }
        // Başka bir uç noktadan IBAN → isim sorgusu yaparak sınır atlatılamaz
        mvc.perform(post("/api/contacts").with(as(eve)).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"x\",\"iban\":\"%s\"}".formatted(target.getIban())))
                .andExpect(status().isTooManyRequests());
        mvc.perform(post("/api/scheduled-transfers").with(as(eve)).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void unrelatedEndpointsAreNotLimited() throws Exception {
        User ada = data.user("Ada");
        for (int i = 0; i < 50; i++) {
            mvc.perform(get("/api/accounts").with(as(ada))).andExpect(status().isOk());
        }
    }

    private ResultActions login(String email) throws Exception {
        return mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"Wrong12345\"}".formatted(email)));
    }
}
