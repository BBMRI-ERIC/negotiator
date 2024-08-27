package eu.bbmri_eric.negotiator.unit.controller;

import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.bbmri_eric.negotiator.governance.resource.ResourceController;
import eu.bbmri_eric.negotiator.governance.resource.ResourceModelAssembler;
import eu.bbmri_eric.negotiator.governance.resource.ResourceService;
import eu.bbmri_eric.negotiator.user.ResourceResponseModel;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
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
  void getResources_noParameters_ok() throws Exception {
    when(resourceService.findAll(any()))
        .thenReturn(new PageImpl<>(List.of(new ResourceResponseModel("idk", "test", "test-name"))));
    mvc.perform(MockMvcRequestBuilders.get("/v3/resources"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page.totalPages", is(1)));
  }
}
