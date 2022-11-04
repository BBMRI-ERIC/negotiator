package eu.bbmri.eric.csit.service.negotiator.api.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.bbmri.eric.csit.service.negotiator.NegotiatorApplication;
import eu.bbmri.eric.csit.service.negotiator.api.v3.TestUtils;
import eu.bbmri.eric.csit.service.negotiator.api.dto.perun.PerunUserRequest;
import eu.bbmri.eric.csit.service.negotiator.api.dto.project.ProjectCreateDTO;
import eu.bbmri.eric.csit.service.negotiator.database.model.Person;
import eu.bbmri.eric.csit.service.negotiator.database.repository.PersonRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(classes = NegotiatorApplication.class)
@ActiveProfiles("test")
@TestMethodOrder(OrderAnnotation.class)
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
    ProjectCreateDTO request = TestUtils.createProjectRequest(false);
    TestUtils.checkErrorResponse(
        mockMvc, HttpMethod.POST, request, status().isUnauthorized(), anonymous(), ENDPOINT);
  }

  @Test
  public void testCreate_Unauthorized_whenWrongAuth() throws Exception {
    ProjectCreateDTO request = TestUtils.createProjectRequest(false);
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
    ProjectCreateDTO request = TestUtils.createProjectRequest(false);
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isForbidden(),
        httpBasic("directory", "directory"),
        ENDPOINT);
  }

  @Test
  @Order(1)
  @Transactional
  public void testCreate_Ok() throws Exception {
    List<PerunUserRequest> request = TestUtils.createPerunUserRequestList(false, 2);
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isCreated(),
        httpBasic("perun", "perun"),
        ENDPOINT);
    request.forEach(pr -> personRepository.deleteByAuthSubject(String.valueOf(pr.getId())));
  }

  @Test
  public void testCreate_BadRequest_WhenOrganizationMissingOrEmpty() throws Exception {
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
  public void testCreate_BadRequest_WhenIdIsNull() throws Exception {
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
  public void testCreate_BadRequest_WhenDisplayNameMissingOrEmpty() throws Exception {
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
  public void testCreate_BadRequest_WhenStatusIsMissingOrEmpty() throws Exception {
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
  public void testCreate_BadRequest_WhenMailIsMissingOrEmpty() throws Exception {
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
  @Order(2)
  @Transactional
  public void testCreate_Ok_WhenIdentitiesAreMissingOrEmpty() throws Exception {
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

    personRepository.deleteByAuthSubject("100");

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

    personRepository.deleteByAuthSubject("100");
  }

  @Test
  public void testUpdate_Ok() throws Exception {
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

    // Check the update in the repository
    Person updatedPerson =
        personRepository.findByAuthSubject(String.valueOf(updateRequest.getId())).orElse(null);
    assertEquals(updatedOrganization, updatedPerson.getOrganization());
  }
}
