package eu.bbmri_eric.negotiator.unit.controller;

import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.bbmri_eric.negotiator.governance.resource.ResourceController;
import eu.bbmri_eric.negotiator.governance.resource.ResourceModelAssembler;
import eu.bbmri_eric.negotiator.governance.resource.ResourceService;
import eu.bbmri_eric.negotiator.governance.resource.dto.ResourceResponseModel;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@WebMvcTest(ResourceController.class)
@Import(ResourceModelAssembler.class)
public class ResourceControllerTest {

  @Autowired private MockMvc mvc;

  @MockBean private ResourceService resourceService;
  @Autowired private ResourceModelAssembler resourceModelAssembler;

  @Test
  void getResources_noAuth_401() throws Exception {
    mvc.perform(MockMvcRequestBuilders.get("/v3/resources")).andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser
  void getResources_withParameters_containsAllCorrectLinks() throws Exception {

    Page<ResourceResponseModel> pageable =
        new PageImpl<>(
            List.of(new ResourceResponseModel(1L, "test", "test-name")), PageRequest.of(1, 1), 3);
    when(resourceService.findAll(any())).thenReturn(pageable);
    mvc.perform(MockMvcRequestBuilders.get("/v3/resources?page=1&name=tes"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page.totalElements", is(3)))
        .andExpect(
            jsonPath(
                "$._links.current.href",
                is("http://localhost/v3/resources?page=1&size=50&name=tes")))
        .andExpect(
            jsonPath(
                "$._links.last.href", is("http://localhost/v3/resources?page=2&size=50&name=tes")))
        .andExpect(
            jsonPath(
                "$._links.first.href", is("http://localhost/v3/resources?page=0&size=50&name=tes")))
        .andExpect(
            jsonPath(
                "$._links.previous.href",
                is("http://localhost/v3/resources?page=0&size=50&name=tes")))
        .andExpect(
            jsonPath(
                "$._links.next.href", is("http://localhost/v3/resources?page=2&size=50&name=tes")));
  }

  @Test
  @WithMockUser
  void getResources_withAllParameters_containsAllCorrectLinks() throws Exception {

    Page<ResourceResponseModel> pageable =
        new PageImpl<>(
            List.of(
                new ResourceResponseModel(
                    1L,
                    "test",
                    "test-name",
                    "test_description",
                    "test@test.org",
                    "http://test.org")),
            PageRequest.of(1, 1),
            3);
    when(resourceService.findAll(any())).thenReturn(pageable);
    mvc.perform(MockMvcRequestBuilders.get("/v3/resources?page=1&name=tes"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page.totalElements", is(3)))
        .andExpect(
            jsonPath(
                "$._links.current.href",
                is("http://localhost/v3/resources?page=1&size=50&name=tes")))
        .andExpect(
            jsonPath(
                "$._links.last.href", is("http://localhost/v3/resources?page=2&size=50&name=tes")))
        .andExpect(
            jsonPath(
                "$._links.first.href", is("http://localhost/v3/resources?page=0&size=50&name=tes")))
        .andExpect(
            jsonPath(
                "$._links.previous.href",
                is("http://localhost/v3/resources?page=0&size=50&name=tes")))
        .andExpect(
            jsonPath(
                "$._links.next.href", is("http://localhost/v3/resources?page=2&size=50&name=tes")));
  }
}
