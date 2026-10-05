package p2p_transfer;

import org.junit.jupiter.api.Test;
import p2p_transfer.support.IntegrationTest;

import static p2p_transfer.support.TestData.as;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;

class P2pTransferApplicationTests extends IntegrationTest {

    @Test
    void healthEndpointIsPublic() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void openApiDocumentDescribesTheApi() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Berq Bank API"))
                .andExpect(jsonPath("$.paths['/api/transactions/transfer']").exists());
    }

    @Test
    void clientSideRoutesFallBackToTheSpaShell() throws Exception {
        mvc.perform(get("/app/transfer"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("berq-test-shell")));
    }

    @Test
    void unknownApiPathsAreNotServedTheSpaShell() throws Exception {
        mvc.perform(get("/api/does-not-exist").with(as(data.user("Zed"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void securityHeadersAreSet() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(header().string("Content-Security-Policy", containsString("frame-ancestors 'none'")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }
}
