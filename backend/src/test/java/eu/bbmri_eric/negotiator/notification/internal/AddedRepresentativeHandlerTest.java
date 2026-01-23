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

  private static final String USER_NOTIFICATIONS_ENDPOINT = "/v3/users/%s/notifications";

  @Test
  @WithMockNegotiatorUser(id = 104L)
  public void handleRepresentativeAddedEvents_whenSingleRepresentative_ok() throws Exception {
    personService.assignAsRepresentativeForResource(104L, 9L);
    personService.assignAsRepresentativeForResource(104L, 10L);
    addedRepresentativeHandler.flushEventBuffer();
    mockMvc
        .perform(get(String.format(USER_NOTIFICATIONS_ENDPOINT, 104L)))
        .andExpect(status().isOk())
        .andDo(print())
        .andExpect(jsonPath("$._embedded.notifications.length()").value(1));
  }

  @Test
  @WithMockNegotiatorUser(id = 105L)
  public void handleRepresentativeAddedEvents_whenDifferentRepresentatives_firstRepresentative_ok()
      throws Exception {
    personService.assignAsRepresentativeForResource(105L, 9L);
    addedRepresentativeHandler.flushEventBuffer();
    mockMvc
        .perform(get(String.format(USER_NOTIFICATIONS_ENDPOINT, 105L)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.notifications.length()").value(1));
  }

  @Test
  @WithMockNegotiatorUser(id = 109L)
  public void handleRepresentativeAddedEvents_whenDifferentRepresentatives_secondRepresentative_ok()
      throws Exception {
    personService.assignAsRepresentativeForResource(109L, 10L);
    addedRepresentativeHandler.flushEventBuffer();
    mockMvc
        .perform(get(String.format(USER_NOTIFICATIONS_ENDPOINT, 109L)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.notifications.length()").value(1));
  }
}
