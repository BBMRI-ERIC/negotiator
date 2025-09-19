package eu.bbmri_eric.negotiator.unit.controller;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.bbmri_eric.negotiator.governance.network.NetworkModelAssembler;
import eu.bbmri_eric.negotiator.governance.network.NetworkService;
import eu.bbmri_eric.negotiator.governance.organization.OrganizationModelAssembler;
import eu.bbmri_eric.negotiator.user.PersonService;
import eu.bbmri_eric.negotiator.user.UserController;
import eu.bbmri_eric.negotiator.user.UserModelAssembler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@WebMvcTest(UserController.class)
@Import(UserModelAssembler.class)
public class UserControllerTest {
  @Autowired private MockMvc mvc;

  @MockBean PersonService personService;

  @MockBean NetworkService networkService;
  @MockBean NetworkModelAssembler networkModelAssembler;
  @MockBean private OrganizationModelAssembler organizationModelAssembler;
  @Autowired private UserModelAssembler userModelAssembler;

  @Test
  void userInfo_okResponse() throws Exception {
    mvc.perform(MockMvcRequestBuilders.get("/v3/userinfo")).andExpect(status().isUnauthorized());
  }
}
