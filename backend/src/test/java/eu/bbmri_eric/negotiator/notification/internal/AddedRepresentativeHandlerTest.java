package eu.bbmri_eric.negotiator.notification.internal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.bbmri_eric.negotiator.user.PersonService;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import eu.bbmri_eric.negotiator.util.WithMockNegotiatorUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
@AutoConfigureMockMvc
@IntegrationTest(loadTestData = true)
public class AddedRepresentativeHandlerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private PersonService personService;

  @Autowired private AddedRepresentativeHandler addedRepresentativeHandler;

  @Test
  @WithMockNegotiatorUser(id = 104L)
  public void handleRepresentativeAddedEvents_whenSingleRepresentative_ok() throws Exception {
    personService.assignAsRepresentativeForResource(104L, 9L);
    personService.assignAsRepresentativeForResource(104L, 10L);
    addedRepresentativeHandler.flushEventBuffer();
    mockMvc
        .perform(get("/v3/users/104/notifications"))
        .andExpect(status().isOk())
        .andDo(print())
        .andExpect(jsonPath("$._embedded.notifications.length()").value(1));
  }

  @Test
  @WithMockNegotiatorUser(id = 105L)
  public void handleRepresentativeAddedEvents_whenDifferentRepresentatives_firstRepresentative_ok()
      throws Exception {
    personService.assignAsRepresentativeForResource(105L, 9L);
    personService.assignAsRepresentativeForResource(109L, 10L);
    addedRepresentativeHandler.flushEventBuffer();
    mockMvc
        .perform(get("/v3/users/105/notifications"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.notifications.length()").value(1));
  }

  @Test
  @WithMockNegotiatorUser(id = 109L)
  public void handleRepresentativeAddedEvents_whenDifferentRepresentatives_secondRepresentative_ok()
      throws Exception {
    addedRepresentativeHandler.flushEventBuffer();
    mockMvc
        .perform(get("/v3/users/109/notifications"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.notifications.length()").value(1));
  }
}
