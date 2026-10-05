package p2p_transfer.contact;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import p2p_transfer.account.Account;
import p2p_transfer.support.IntegrationTest;
import p2p_transfer.user.User;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static p2p_transfer.support.TestData.as;

class ContactApiTests extends IntegrationTest {

    @Test
    void fullLifecycle() throws Exception {
        User ada = data.user("Ada");
        Account bobAcc = data.account(data.user("Bob"), "0");

        String body = create(ada, "Bobby", bobAcc.getIban())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerName").value("Bob T***"))
                .andReturn().getResponse().getContentAsString();
        Integer id = JsonPath.read(body, "$.id");

        create(ada, "Again", bobAcc.getIban())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONTACT_EXISTS"));

        mvc.perform(patch("/api/contacts/{id}", id).with(as(ada)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nickname\":\"Bob abi\"}"))
                .andExpect(jsonPath("$.nickname").value("Bob abi"));

        mvc.perform(get("/api/contacts").with(as(ada)))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].iban").value(bobAcc.getIban()));

        mvc.perform(delete("/api/contacts/{id}", id).with(as(ada)).with(csrf()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/contacts").with(as(ada)))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void cannotSaveOwnAccountOrTouchOthersContacts() throws Exception {
        User ada = data.user("Ada");
        User eve = data.user("Eve");
        Account adaAcc = data.account(ada, "0");
        Account bobAcc = data.account(data.user("Bob"), "0");

        create(ada, "Me", adaAcc.getIban())
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("CONTACT_IS_SELF"));

        Integer id = JsonPath.read(create(ada, "Bob", bobAcc.getIban()).andReturn().getResponse().getContentAsString(), "$.id");
        mvc.perform(delete("/api/contacts/{id}", id).with(as(eve)).with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CONTACT_NOT_FOUND"));
    }

    private ResultActions create(User owner, String nickname, String iban) throws Exception {
        return mvc.perform(post("/api/contacts").with(as(owner)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nickname\":\"%s\",\"iban\":\"%s\"}".formatted(nickname, iban)));
    }
}
