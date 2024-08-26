package eu.bbmri_eric.negotiator.unit.controller;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.bbmri_eric.negotiator.governance.resource.ResourceController;
import eu.bbmri_eric.negotiator.governance.resource.ResourceModelAssembler;
import eu.bbmri_eric.negotiator.governance.resource.ResourceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@WebMvcTest(ResourceController.class)
public class ResourceControllerTest {

  @Autowired private MockMvc mvc;

  @MockBean private ResourceService resourceService;

  @MockBean private ResourceModelAssembler resourceModelAssembler;

  @Test
  void getResources_noAuth_401() throws Exception {
    mvc.perform(MockMvcRequestBuilders.get("/v3/resources")).andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser
  void getResources_filterByNonExistentFilter_400() throws Exception {
    mvc.perform(MockMvcRequestBuilders.get("/v3/resources?name=resource"))
        .andExpect(status().isBadRequest());
  }
}
