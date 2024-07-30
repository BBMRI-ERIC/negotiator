package eu.bbmri_eric.negotiator.integration.api.v3;

import static org.hamcrest.core.Is.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.negotiator.NegotiatorApplication;
import eu.bbmri_eric.negotiator.configuration.state_machine.resource.NegotiationResourceEvent;
import eu.bbmri_eric.negotiator.database.model.Negotiation;
import eu.bbmri_eric.negotiator.database.repository.InformationRequirementRepository;
import eu.bbmri_eric.negotiator.database.repository.NegotiationRepository;
import eu.bbmri_eric.negotiator.dto.InformationRequirementCreateDTO;
import eu.bbmri_eric.negotiator.dto.InformationRequirementDTO;
import eu.bbmri_eric.negotiator.dto.InformationSubmissionDTO;
import eu.bbmri_eric.negotiator.service.InformationRequirementServiceImpl;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(classes = NegotiatorApplication.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class InformationRequirementControllerTest {
  private final String INFO_REQUIREMENT_ENDPOINT = "/v3/info-requirements";
  private final String INFO_SUBMISSION_ENDPOINT = "/v3/negotiations/%s/info-requirements/%s";
  private final String SUBMISSION_ENDPOINT = "/v3/info-submissions/%s";
  private MockMvc mockMvc;
  @Autowired private NegotiationRepository negotiationRepository;
  @Autowired private InformationRequirementRepository informationRequirementRepository;
  @Autowired private InformationRequirementServiceImpl informationRequirementServiceImpl;

  @BeforeEach
  void setup(WebApplicationContext wac) {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
  }

  @Test
  @WithMockUser
  void createInformationRequirement_null_returns400() throws Exception {
    InformationRequirementCreateDTO createDTO = new InformationRequirementCreateDTO(null, null);
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(INFO_REQUIREMENT_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(createDTO)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title", is("Incorrect parameters")));
  }

  @Test
  @WithMockUser
  void createInformationRequirement_correctBody_ok() throws Exception {
    InformationRequirementCreateDTO createDTO =
        new InformationRequirementCreateDTO(1L, NegotiationResourceEvent.CONTACT);
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(INFO_REQUIREMENT_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(createDTO)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.forResourceEvent", is("CONTACT")))
        .andExpect(jsonPath("$._links").isNotEmpty());
  }

  @Test
  @WithMockUser
  void deleteInformationRequirement_existingId_ok() throws Exception {
    InformationRequirementCreateDTO createDTO =
        new InformationRequirementCreateDTO(1L, NegotiationResourceEvent.CONTACT);
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(INFO_REQUIREMENT_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(createDTO)))
        .andExpect(status().isCreated());
    long idToBeDeleted = 1L; // Replace with actual id from your setup
    mockMvc
        .perform(MockMvcRequestBuilders.delete(INFO_REQUIREMENT_ENDPOINT + "/" + idToBeDeleted))
        .andExpect(status().isOk());
  }

  @Test
  @WithMockUser
  void deleteInformationRequirement_nonExistingId_notFound() throws Exception {
    long nonExistingId = 999L;
    mockMvc
        .perform(MockMvcRequestBuilders.delete(INFO_REQUIREMENT_ENDPOINT + "/" + nonExistingId))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser
  void updateRequirement_null_returns400() throws Exception {
    InformationRequirementCreateDTO createDTO = new InformationRequirementCreateDTO(null, null);
    mockMvc
        .perform(
            MockMvcRequestBuilders.put(INFO_REQUIREMENT_ENDPOINT + "/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(createDTO)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title", is("Incorrect parameters")));
  }

  @Test
  @WithMockUser
  void updateRequirement_correctBody_ok() throws Exception {
    InformationRequirementCreateDTO createDTO =
        new InformationRequirementCreateDTO(1L, NegotiationResourceEvent.CONTACT);
    MvcResult mvcResult =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post(INFO_REQUIREMENT_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(new ObjectMapper().writeValueAsString(createDTO)))
            .andExpect(status().isCreated())
            .andReturn();
    long id =
        new ObjectMapper()
            .readTree(mvcResult.getResponse().getContentAsString())
            .get("id")
            .asLong();
    createDTO = new InformationRequirementCreateDTO(1L, NegotiationResourceEvent.STEP_AWAY);
    mockMvc
        .perform(
            MockMvcRequestBuilders.put(INFO_REQUIREMENT_ENDPOINT + "/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(createDTO)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.forResourceEvent", is("STEP_AWAY")))
        .andExpect(jsonPath("$._links").isNotEmpty());
  }

  @Test
  @WithMockUser
  void findAllRequirements_noExist_returnsEmptyArray() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get(INFO_REQUIREMENT_ENDPOINT))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._links").isNotEmpty());
  }

  @Test
  @WithMockUser
  void findAllRequirements_twoExist_returnsArrayWithTwoElements() throws Exception {
    InformationRequirementCreateDTO createDTO1 =
        new InformationRequirementCreateDTO(1L, NegotiationResourceEvent.CONTACT);
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(INFO_REQUIREMENT_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(createDTO1)))
        .andExpect(status().isCreated());

    InformationRequirementCreateDTO createDTO2 =
        new InformationRequirementCreateDTO(1L, NegotiationResourceEvent.STEP_AWAY);
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(INFO_REQUIREMENT_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(createDTO2)))
        .andExpect(status().isCreated());

    mockMvc
        .perform(MockMvcRequestBuilders.get(INFO_REQUIREMENT_ENDPOINT))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.info-requirements", Matchers.hasSize(2)))
        .andExpect(jsonPath("$._links").isNotEmpty());
  }

  @Test
  @WithMockUser
  void updateRequirement_nonExistingId_notFound() throws Exception {
    InformationRequirementCreateDTO createDTO =
        new InformationRequirementCreateDTO(1L, NegotiationResourceEvent.CONTACT);
    mockMvc
        .perform(
            MockMvcRequestBuilders.put(INFO_REQUIREMENT_ENDPOINT + "/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(createDTO)))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser
  void findRequirementById_existingId_ok() throws Exception {
    InformationRequirementCreateDTO createDTO =
        new InformationRequirementCreateDTO(1L, NegotiationResourceEvent.CONTACT);
    MvcResult mvcResult =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post(INFO_REQUIREMENT_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(new ObjectMapper().writeValueAsString(createDTO)))
            .andExpect(status().isCreated())
            .andReturn();
    long id =
        new ObjectMapper()
            .readTree(mvcResult.getResponse().getContentAsString())
            .get("id")
            .asLong();
    mockMvc
        .perform(MockMvcRequestBuilders.get(INFO_REQUIREMENT_ENDPOINT + "/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.forResourceEvent", is("CONTACT")))
        .andExpect(jsonPath("$._links").isNotEmpty());
  }

  @Test
  @WithMockUser
  void findRequirementById_nonExistingId_notFound() throws Exception {
    long nonExistingId = 999L;
    mockMvc
        .perform(MockMvcRequestBuilders.get(INFO_REQUIREMENT_ENDPOINT + "/" + nonExistingId))
        .andExpect(status().isNotFound());
  }

  @Test
  void getSummary_nonExistingId_notFound() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get(INFO_SUBMISSION_ENDPOINT.formatted("1", "1")))
        .andExpect(status().isNotFound());
  }

  @Test
  void getSummary_nonExistingRequirement_notFound() throws Exception {
    Negotiation negotiation = negotiationRepository.findAll().iterator().next();
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(
                INFO_SUBMISSION_ENDPOINT.formatted(negotiation.getId(), "1")))
        .andExpect(status().isNotFound());
  }

  @Test
  void getSummary_noSubmissions_emptyResponse() throws Exception {
    Negotiation negotiation = negotiationRepository.findAll().iterator().next();
    InformationRequirementDTO informationRequirementDTO =
        informationRequirementServiceImpl.createInformationRequirement(
            new InformationRequirementCreateDTO(1L, NegotiationResourceEvent.CONTACT));
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(
                INFO_SUBMISSION_ENDPOINT.formatted(
                    negotiation.getId(), informationRequirementDTO.getId())))
        .andExpect(status().isOk())
        .andExpect(content().json("{}"));
  }

  @Test
  void submitInformation_correctPayload_ok() throws Exception {
    Negotiation negotiation = negotiationRepository.findAll().iterator().next();
    InformationRequirementDTO informationRequirementDTO =
        informationRequirementServiceImpl.createInformationRequirement(
            new InformationRequirementCreateDTO(1L, NegotiationResourceEvent.CONTACT));
    String payload =
        """
                        {
                       "sample-type": "DNA",
                       "num-of-subjects": 10,
                       "num-of-samples": 20,
                       "volume-per-sample": 5
                    }
                    """;
    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonPayload = mapper.readTree(payload);
    InformationSubmissionDTO submissionDTO = new InformationSubmissionDTO(4L, jsonPayload);
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(
                    INFO_SUBMISSION_ENDPOINT.formatted(
                        negotiation.getId(), informationRequirementDTO.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(submissionDTO)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.resourceId").value(submissionDTO.getResourceId()))
        .andExpect(jsonPath("$.payload.sample-type").value("DNA"));
  }

  @Test
  void getSubmission_exists_ok() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get(SUBMISSION_ENDPOINT.formatted("1")))
        .andExpect(status().isOk());
  }
}
