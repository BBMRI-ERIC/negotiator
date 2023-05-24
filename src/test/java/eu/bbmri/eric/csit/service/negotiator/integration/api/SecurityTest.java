package eu.bbmri.eric.csit.service.negotiator.integration.api;

import eu.bbmri.eric.csit.service.negotiator.NegotiatorApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = NegotiatorApplication.class)
@ActiveProfiles("test")
public class SecurityTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    public void beforeAll() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }
    @Test
    void testUnauthenticatedUser() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/v3/negotiations")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser()
    void testAuthorized() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/v3/negotiations")).andExpect(status().isOk());
    }

    @Test
    void testAuthenticatedButMethodNotAllowed() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/v3/access-criteria")).andExpect(status().isForbidden());
    }
}
