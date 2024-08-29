package eu.bbmri_eric.negotiator.integration.api.v3;

import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.negotiator.common.AuthenticatedUserContext;
import eu.bbmri_eric.negotiator.info_requirement.InformationRequirementCreateDTO;
import eu.bbmri_eric.negotiator.info_requirement.InformationRequirementDTO;
import eu.bbmri_eric.negotiator.info_requirement.InformationRequirementRepository;
import eu.bbmri_eric.negotiator.info_requirement.InformationRequirementServiceImpl;
import eu.bbmri_eric.negotiator.info_submission.InformationSubmission;
import eu.bbmri_eric.negotiator.info_submission.InformationSubmissionDTO;
import eu.bbmri_eric.negotiator.info_submission.InformationSubmissionRepository;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceEvent;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import eu.bbmri_eric.negotiator.util.WithMockNegotiatorUser;
import jakarta.transaction.Transactional;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@IntegrationTest(loadTestData = true)
public class InformationRequirementControllerTest {
  private final String INFO_REQUIREMENT_ENDPOINT = "/v3/info-requirements";
  private final String INFO_SUBMISSION_ENDPOINT = "/v3/negotiations/%s/info-requirements/%s";
  private final String SUBMISSION_ENDPOINT = "/v3/info-submissions/%s";
  private MockMvc mockMvc;
  @Autowired private NegotiationRepository negotiationRepository;
  @Autowired private InformationRequirementRepository informationRequirementRepository;
  @Autowired private InformationSubmissionRepository informationSubmissionRepository;
  @Autowired private InformationRequirementServiceImpl informationRequirementServiceImpl;

  @BeforeEach
  void setup(WebApplicationContext wac) {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(wac).apply(springSecurity()).build();
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void createInformationRequirement_null_returns400() throws Exception {
    InformationRequirementCreateDTO createDTO = new InformationRequirementCreateDTO(null, null);
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(INFO_REQUIREMENT_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(createDTO)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title", is("Wrong request parameters")));
  }

  @Test
  void createInformationRequirement_notAdmin_403() throws Exception {
    InformationRequirementCreateDTO createDTO =
        new InformationRequirementCreateDTO(1L, NegotiationResourceEvent.CONTACT);
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(INFO_REQUIREMENT_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(createDTO)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
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
  @WithMockUser(roles = "ADMIN")
  void createInformationRequirement_onlyForAdmin_ok() throws Exception {
    InformationRequirementCreateDTO createDTO =
        new InformationRequirementCreateDTO(1L, NegotiationResourceEvent.CONTACT, false);
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(INFO_REQUIREMENT_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(createDTO)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.forResourceEvent", is("CONTACT")))
        .andExpect(jsonPath("$.viewableOnlyByAdmin", is(false)))
        .andExpect(jsonPath("$._links").isNotEmpty());
    createDTO = new InformationRequirementCreateDTO(1L, NegotiationResourceEvent.CONTACT);
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(INFO_REQUIREMENT_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(createDTO)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.forResourceEvent", is("CONTACT")))
        .andExpect(jsonPath("$.viewableOnlyByAdmin", is(true)))
        .andExpect(jsonPath("$._links").isNotEmpty());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
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
  @WithMockUser(roles = "ADMIN")
  void deleteInformationRequirement_nonExistingId_notFound() throws Exception {
    long nonExistingId = 999L;
    mockMvc
        .perform(MockMvcRequestBuilders.delete(INFO_REQUIREMENT_ENDPOINT + "/" + nonExistingId))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void updateRequirement_null_returns400() throws Exception {
    InformationRequirementCreateDTO createDTO = new InformationRequirementCreateDTO(null, null);
    mockMvc
        .perform(
            MockMvcRequestBuilders.put(INFO_REQUIREMENT_ENDPOINT + "/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(createDTO)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title", is("Wrong request parameters")));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
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
  @WithMockUser(roles = "ADMIN")
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
  @WithMockUser(roles = "ADMIN")
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
  @WithMockUser(roles = "ADMIN")
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
  @WithUserDetails("TheBiobanker")
  @Transactional
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
    InformationSubmissionDTO submissionDTO =
        new InformationSubmissionDTO(
            negotiation.getResources().iterator().next().getId(), jsonPayload);
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
  @WithUserDetails("researcher")
  @Transactional
  void getSubmission_existsButNotAuth_403() throws Exception {
    Negotiation negotiation = negotiationRepository.findAll().iterator().next();
    InformationSubmission informationSubmission =
        informationSubmissionRepository.saveAndFlush(
            new InformationSubmission(
                null, negotiation.getResources().iterator().next(), negotiation, "{}"));
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(
                SUBMISSION_ENDPOINT.formatted(informationSubmission.getId())))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithUserDetails("TheResearcher")
  @Transactional
  void getSubmission_existsAsRequestAuthor_ok() throws Exception {
    Negotiation negotiation = negotiationRepository.findById("negotiation-1").get();
    assertEquals(
        negotiation.getCreatedBy().getId(),
        AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId());
    InformationSubmission informationSubmission =
        informationSubmissionRepository.saveAndFlush(
            new InformationSubmission(
                null, negotiation.getResources().iterator().next(), negotiation, "{}"));
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(
                SUBMISSION_ENDPOINT.formatted(informationSubmission.getId())))
        .andExpect(status().isOk());
  }

  @Test
  @WithUserDetails("TheResearcher")
  @Transactional
  void getSubmission_existsAsRepresentative_ok() throws Exception {
    Negotiation negotiation = negotiationRepository.findById("negotiation-1").get();
    InformationSubmission informationSubmission =
        informationSubmissionRepository.saveAndFlush(
            new InformationSubmission(
                null,
                negotiation.getResources().stream()
                    .filter(resource -> resource.getId().equals(4L))
                    .findFirst()
                    .get(),
                negotiation,
                "{}"));
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(
                SUBMISSION_ENDPOINT.formatted(informationSubmission.getId())))
        .andExpect(status().isOk());
  }

  @Test
  @WithUserDetails("TheResearcher")
  @Transactional
  void submit_notARepresentative_403() throws Exception {
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
    InformationSubmissionDTO submissionDTO =
        new InformationSubmissionDTO(
            negotiation.getResources().iterator().next().getId(), jsonPayload);
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(
                    INFO_SUBMISSION_ENDPOINT.formatted(
                        negotiation.getId(), informationRequirementDTO.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(submissionDTO)))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithUserDetails("TheBiobanker")
  @Transactional
  void submit_2forTheSameRequirement_400() throws Exception {
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
    InformationSubmissionDTO submissionDTO =
        new InformationSubmissionDTO(
            negotiation.getResources().iterator().next().getId(), jsonPayload);
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(
                    INFO_SUBMISSION_ENDPOINT.formatted(
                        negotiation.getId(), informationRequirementDTO.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(submissionDTO)))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(
                    INFO_SUBMISSION_ENDPOINT.formatted(
                        negotiation.getId(), informationRequirementDTO.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(submissionDTO)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockNegotiatorUser(id = 109L)
  void generateSummary_representative_403() throws Exception {
    Negotiation negotiation = negotiationRepository.findAll().iterator().next();
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(
                INFO_SUBMISSION_ENDPOINT.formatted(negotiation.getId(), 9999L)))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockNegotiatorUser(id = 109L, authorities = "ROLE_ADMIN")
  void generateSummary_nonExistentRequirement_404() throws Exception {
    Negotiation negotiation = negotiationRepository.findAll().iterator().next();
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(
                INFO_SUBMISSION_ENDPOINT.formatted(negotiation.getId(), 9999L)))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockNegotiatorUser(id = 109L, authorities = "ROLE_ADMIN")
  void generateSummary_noSubmissions_404() throws Exception {
    Negotiation negotiation = negotiationRepository.findAll().iterator().next();
    InformationRequirementDTO requirementDTO =
        informationRequirementServiceImpl.createInformationRequirement(
            new InformationRequirementCreateDTO(1L, NegotiationResourceEvent.CONTACT));
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(
                INFO_SUBMISSION_ENDPOINT.formatted(negotiation.getId(), requirementDTO.getId())))
        .andExpect(status().isOk())
        .andExpect(
            header()
                .string(
                    HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"%s-summary.csv\""
                        .formatted(requirementDTO.getRequiredAccessForm().getName())))
        .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "text/csv"))
        .andExpect(content().string(""));
  }

  @Test
  @WithMockNegotiatorUser(id = 109L, authorities = "ROLE_ADMIN")
  @Transactional
  void generateSummary_1submission_ok() throws Exception {
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
    InformationSubmissionDTO submissionDTO =
        new InformationSubmissionDTO(
            negotiation.getResources().iterator().next().getId(), jsonPayload);
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(
                    INFO_SUBMISSION_ENDPOINT.formatted(
                        negotiation.getId(), informationRequirementDTO.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(submissionDTO)))
        .andExpect(status().isOk());
    String expectedResponse =
        """
resourceId,num-of-samples,num-of-subjects,sample-type,volume-per-sample
biobank:1:collection:1,20,10,DNA,5
""";
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(
                INFO_SUBMISSION_ENDPOINT.formatted(
                    negotiation.getId(), informationRequirementDTO.getId())))
        .andExpect(status().isOk())
        .andExpect(
            header()
                .string(
                    HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"%s-summary.csv\""
                        .formatted(informationRequirementDTO.getRequiredAccessForm().getName())))
        .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "text/csv"))
        .andExpect(content().string(normalizeLineEndingsToCRLF(expectedResponse)));
  }

  private String normalizeLineEndingsToCRLF(String text) {
    return text.replace("\r\n", "\n").replace("\n", "\r\n");
  }
}
