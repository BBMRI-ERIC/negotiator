package negotiator.api.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.bbmri.eric.csit.service.negotiator.NegotiatorApplication;
import eu.bbmri.eric.csit.service.negotiator.dto.request.PerunUserRequest;
import eu.bbmri.eric.csit.service.negotiator.dto.request.ProjectRequest;
import eu.bbmri.eric.csit.service.negotiator.model.Person;
import eu.bbmri.eric.csit.service.negotiator.repository.PersonRepository;
import java.util.List;
import negotiator.api.v3.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(classes = NegotiatorApplication.class)
@ActiveProfiles("test")
public class PerunControllerTests {
  private static final String ENDPOINT = "/perun/users";
  private MockMvc mockMvc;
  @Autowired private WebApplicationContext context;
  @Autowired private PersonRepository personRepository;
  @Autowired private ModelMapper modelMapper;

  @BeforeEach
  public void before() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @Test
  public void testCreate_Unauthorized_whenNoAuth() throws Exception {
    ProjectRequest request = TestUtils.createProjectRequest(false);
    TestUtils.checkErrorResponse(
        mockMvc, HttpMethod.POST, request, status().isUnauthorized(), anonymous(), ENDPOINT);
  }

  @Test
  public void testCreate_Unauthorized_whenWrongAuth() throws Exception {
    ProjectRequest request = TestUtils.createProjectRequest(false);
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isUnauthorized(),
        httpBasic("perun", "wrong_pass"),
        ENDPOINT);
  }

  @Test
  public void testCreate_Forbidden_whenNoPermission() throws Exception {
    ProjectRequest request = TestUtils.createProjectRequest(false);
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isForbidden(),
        httpBasic("directory", "directory"),
        ENDPOINT);
  }

  @Test
  public void testCreate_goodRequest() throws Exception {
    List<PerunUserRequest> request = TestUtils.createPerunUserRequestList(false, 2);
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isCreated(),
        httpBasic("perun", "perun"),
        ENDPOINT);
  }

  @Test
  public void testCreate_organization_missing_or_empty() throws Exception {
    List<PerunUserRequest> request = TestUtils.createPerunUserRequestList(false, 2);
    // for one of the requests, set organization as null
    PerunUserRequest badRequest = request.get(0);
    badRequest.setOrganization(null);
    request.set(0, badRequest);
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        httpBasic("perun", "perun"),
        ENDPOINT);
    badRequest.setOrganization("");
    request.set(0, badRequest);
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        httpBasic("perun", "perun"),
        ENDPOINT);
  }

  @Test
  public void testCreate_id_null() throws Exception {
    List<PerunUserRequest> request = TestUtils.createPerunUserRequestList(false, 2);
    // for one of the requests, set organization as null
    PerunUserRequest badRequest = request.get(0);
    badRequest.setId(null);
    request.set(0, badRequest);
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        httpBasic("perun", "perun"),
        ENDPOINT);
  }

  @Test
  public void testCreate_displayName_missing_or_empty() throws Exception {
    List<PerunUserRequest> request = TestUtils.createPerunUserRequestList(false, 2);
    // for one of the requests, set organization as null
    PerunUserRequest badRequest = request.get(0);
    badRequest.setDisplayName(null);
    request.set(0, badRequest);
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        httpBasic("perun", "perun"),
        ENDPOINT);
    badRequest.setDisplayName("");
    request.set(0, badRequest);
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        httpBasic("perun", "perun"),
        ENDPOINT);
  }

  @Test
  public void testCreate_status_missing_or_empty() throws Exception {
    List<PerunUserRequest> request = TestUtils.createPerunUserRequestList(false, 2);
    // for one of the requests, set organization as null
    PerunUserRequest badRequest = request.get(0);
    badRequest.setStatus(null);
    request.set(0, badRequest);
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        httpBasic("perun", "perun"),
        ENDPOINT);
    badRequest.setStatus("");
    request.set(0, badRequest);
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        httpBasic("perun", "perun"),
        ENDPOINT);
  }

  @Test
  public void testCreate_mail_missing_or_empty() throws Exception {
    List<PerunUserRequest> request = TestUtils.createPerunUserRequestList(false, 2);
    // for one of the requests, set organization as null
    PerunUserRequest badRequest = request.get(0);
    badRequest.setMail(null);
    request.set(0, badRequest);
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        httpBasic("perun", "perun"),
        ENDPOINT);
    badRequest.setMail("");
    request.set(0, badRequest);
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        httpBasic("perun", "perun"),
        ENDPOINT);
  }

  @Test
  public void testCreate_identities_missing_or_empty() throws Exception {
    List<PerunUserRequest> request = TestUtils.createPerunUserRequestList(false, 2);
    // for one of the requests, set organization as null
    PerunUserRequest badRequest = request.get(0);
    badRequest.setIdentities(null);
    request.set(0, badRequest);
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isCreated(),
        httpBasic("perun", "perun"),
        ENDPOINT);
    String[] emptyIdentities = {};
    badRequest.setIdentities(emptyIdentities);
    request.set(0, badRequest);
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isCreated(),
        httpBasic("perun", "perun"),
        ENDPOINT);
  }

  @Test
  public void testUpdate_goodRequest() throws Exception {
    List<PerunUserRequest> request = TestUtils.createPerunUserRequestList(false, 1);
    Person personEntity = modelMapper.map(request.get(0), Person.class);
    personRepository.save(personEntity);
    // Update by changing the organization in the request
    PerunUserRequest updateRequest = request.get(0);
    String updatedOrganization = "UpdatedOrganization";
    updateRequest.setOrganization(updatedOrganization);
    request.set(0, updateRequest);
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isCreated(),
        httpBasic("perun", "perun"),
        ENDPOINT);

    // Check the upfare in the repository
    Person updatedPerson =
        personRepository.findByAuthSubject(String.format("%s", updateRequest.getId())).orElse(null);
    assertEquals(updatedOrganization, updatedPerson.getOrganization());
  }
}
