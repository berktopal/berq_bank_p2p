package p2p_transfer.auth;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import p2p_transfer.support.IntegrationTest;
import p2p_transfer.support.TestData;
import p2p_transfer.user.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static p2p_transfer.support.Money.eq;

class AuthApiTests extends IntegrationTest {

    private static final String REGISTER = """
            {"firstName":"Berk","lastName":"Topal","tckn":"10000000146","email":"Berk@Example.com","password":"Secret123"}""";

    @Test
    void registerCreatesUserOpensFirstAccountAndSignsIn() throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(REGISTER))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("berk@example.com"))
                .andExpect(jsonPath("$.tckn").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        mvc.perform(get("/api/accounts").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].currency").value("TRY"))
                .andExpect(jsonPath("$[0].balance", eq("0")))
                .andExpect(jsonPath("$[0].iban", matchesPattern("TR\\d{24}")));

        String hash = jdbc.queryForObject("select password from users where email = 'berk@example.com'", String.class);
        assertThat(hash).startsWith("$2").doesNotContain("Secret123");
    }

    @Test
    void emailUniquenessIsCaseInsensitive() throws Exception {
        register(REGISTER).andExpect(status().isCreated());
        register(REGISTER.replace("Berk@Example.com", "BERK@example.COM").replace("10000000146", "11111111110"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_TAKEN"));
    }

    @Test
    void registrationValidatesTcknAndPasswordStrength() throws Exception {
        register(REGISTER.replace("10000000146", "12345678901").replace("Secret123", "short"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.tckn").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void loginMeLogoutRoundTrip() throws Exception {
        User ada = data.user("Ada");
        MvcResult result = login("ada@test.local", TestData.PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Ada"))
                .andReturn();
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);

        mvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ada.getId()));

        mvc.perform(post("/api/auth/logout").session(session).with(csrf()))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void wrongPasswordAndUnknownEmailAreIndistinguishable() throws Exception {
        data.user("Ada");
        login("ada@test.local", "wrong-pass1").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        login("nobody@test.local", "wrong-pass1").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void accountIsLockedAfterRepeatedFailuresEvenForTheRightPassword() throws Exception {
        data.user("Ada");
        for (int i = 0; i < 4; i++) {
            login("ada@test.local", "wrong-pass1").andExpect(status().isUnauthorized());
        }
        login("ada@test.local", "wrong-pass1").andExpect(status().isLocked())
                .andExpect(jsonPath("$.code").value("USER_LOCKED"))
                .andExpect(jsonPath("$.lockedUntil").exists());
        login("ada@test.local", TestData.PASSWORD).andExpect(status().isLocked());
    }

    @Test
    void stateChangingRequestsRequireCsrfToken() throws Exception {
        data.user("Ada");
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ada@test.local\",\"password\":\"" + TestData.PASSWORD + "\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void protectedEndpointsReturnJsonProblemWhenAnonymous() throws Exception {
        mvc.perform(get("/api/accounts"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void changePasswordRequiresCurrentPassword() throws Exception {
        User ada = data.user("Ada");
        mvc.perform(post("/api/profile/password").with(TestData.as(ada)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"nope\",\"newPassword\":\"NewSecret9\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WRONG_CURRENT_PASSWORD"));

        mvc.perform(post("/api/profile/password").with(TestData.as(ada)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"" + TestData.PASSWORD + "\",\"newPassword\":\"NewSecret9\"}"))
                .andExpect(status().isNoContent());

        login("ada@test.local", "NewSecret9").andExpect(status().isOk());
    }

    @Test
    void profileMasksTckn() throws Exception {
        User ada = data.user("Ada");
        mvc.perform(get("/api/profile").with(TestData.as(ada)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maskedTckn", matchesPattern("\\d{3}\\*{6}\\d{2}")))
                .andExpect(jsonPath("$.tckn").doesNotExist());
    }

    private ResultActions register(String body) throws Exception {
        return mvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions login(String email, String password) throws Exception {
        return mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"));
    }
}
