package eu.bbmri_eric.negotiator.integration.api.v3;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.discovery_service.DiscoveryServiceCreateDTO;
import eu.bbmri_eric.negotiator.negotiation.dto.NegotiationCreateDTO;
import eu.bbmri_eric.negotiator.negotiation.dto.CollectionV2DTO;
import eu.bbmri_eric.negotiator.negotiation.dto.QueryCreateV2DTO;
import eu.bbmri_eric.negotiator.negotiation.dto.RequestCreateDTO;
import eu.bbmri_eric.negotiator.governance.OrganizationDTO;
import eu.bbmri_eric.negotiator.governance.organization.Organization;
import eu.bbmri_eric.negotiator.governance.resource.dto.ResourceDTO;
import eu.bbmri_eric.negotiator.post.Post;
import eu.bbmri_eric.negotiator.post.PostCreateDTO;
import eu.bbmri_eric.negotiator.post.PostStatus;
import eu.bbmri_eric.negotiator.post.PostType;
import eu.bbmri_eric.negotiator.user.Person;
import java.io.IOException;
import java.net.URI;
import java.util.Set;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

public class TestUtils {

  public static final String DISCOVERY_SERVICE_NAME = "Discovery Service";
  public static final String DISCOVERY_SERVICE_URL = "http://discoveryservice.test";
  public static final String QUERY_URL = "http://discoveryservice.dev";
  public static final String QUERY_HUMAN_READABLE = "Request description";
  public static final String QUERY_BIOBANK_1_ID = "biobank:1";
  public static final String QUERY_BIOBANK_1_NAME = "Test Biobank #1";
  public static final String QUERY_COLLECTION_1_ID = "biobank:1:collection:1";
  public static final String QUERY_COLLECTION_1_NAME = "Test collection #1 of biobank #1";
  public static final String QUERY_BIOBANK_2_ID = "biobank:2";
  public static final String QUERY_BIOBANK_2_NAME = "Test Biobank 2 #2";
  public static final String QUERY_COLLECTION_2_ID = "biobank:2:collection:1";
  public static final String QUERY_COLLECTION_2_NAME = "Test collection #1 of biobank #2";

  public static final String PROJECT_PAYLOAD =
      "{\"title\":\"Test project\",\"description\":\"This is a test project\"}";

  public static final String REQUEST_TITLE = "negotiation title";
  public static final String REQUEST_DESCRIPTION = "negotiation description";

  public static final String PERUN_USER_ORGANIZATION = "perun user organization";
  public static final Integer PERUN_USER_ID = 100;
  public static final String PERUN_USER_DISPLAY_NAME = "display name";
  public static final String PERUN_USER_STATUS = "perun user status";
  public static final String PERUN_USER_MAIL = "perunusermail@mail.it";
  public static final String[] PERUN_USER_IDENTITIES = {
    "perun user identity 1", "perun user identity 2"
  };

  public static DiscoveryServiceCreateDTO createDiscoveryServiceRequest(boolean update) {
    String suffix = update ? "u" : "";
    return DiscoveryServiceCreateDTO.builder()
        .name(String.format("%s%s", DISCOVERY_SERVICE_NAME, suffix))
        .url(String.format("%s%s", DISCOVERY_SERVICE_URL, suffix))
        .build();
  }

  public static RequestCreateDTO createRequest(boolean update) {
    String suffix = update ? "u" : "";
    String collectionId = update ? QUERY_COLLECTION_2_ID : QUERY_COLLECTION_1_ID;
    String collectionName = update ? QUERY_COLLECTION_2_NAME : QUERY_COLLECTION_1_NAME;
    String biobankId = update ? QUERY_BIOBANK_2_ID : QUERY_BIOBANK_1_ID;
    String biobankName = update ? QUERY_BIOBANK_2_NAME : QUERY_BIOBANK_1_NAME;

    ResourceDTO collection =
        ResourceDTO.builder()
            .id(collectionId)
            .name(collectionName)
            .organization(OrganizationDTO.builder().externalId(biobankId).name(biobankName).build())
            .build();

    return RequestCreateDTO.builder()
        .humanReadable(String.format("%s%s", QUERY_HUMAN_READABLE, suffix))
        .url(QUERY_URL)
        .resources(Set.of(collection))
        .build();
  }

  public static QueryCreateV2DTO createQueryV2Request() {
    CollectionV2DTO collection =
        CollectionV2DTO.builder()
            .collectionId(QUERY_COLLECTION_1_ID)
            .biobankId(QUERY_BIOBANK_1_ID)
            .build();

    return QueryCreateV2DTO.builder()
        .humanReadable(QUERY_HUMAN_READABLE)
        .url(QUERY_URL)
        .collections(Set.of(collection))
        .build();
  }

  public static NegotiationCreateDTO createNegotiation(Set<String> requestsId) throws IOException {
    String payload =
        "    {\n"
            + "\"project\": {\n"
            + "\"title\": \"Title\",\n"
            + "\"description\": \"Description\"\n"
            + "},\n"
            + " \"samples\": {\n"
            + "   \"sample-type\": \"DNA\",\n"
            + "   \"num-of-subjects\": 10,\n"
            + "   \"num-of-samples\": 20,\n"
            + "   \"volume-per-sample\": 5\n"
            + " },\n"
            + " \"ethics-vote\": {\n"
            + "   \"ethics-vote\": \"My ethic vote\"\n"
            + " }\n"
            + "}\n";
    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonPayload = mapper.readTree(payload);

    return NegotiationCreateDTO.builder().payload(jsonPayload).requests(requestsId).build();
  }

  public static String jsonFromRequest(Object request) throws JsonProcessingException {
    ObjectMapper mapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
    return mapper.writeValueAsString(request);
  }

  public static void checkErrorResponse(
      MockMvc mockMvc,
      HttpMethod method,
      Object request,
      ResultMatcher statusMatcher,
      RequestPostProcessor auth,
      String endpoint)
      throws Exception {
    String requestBody = TestUtils.jsonFromRequest(request);
    checkErrorResponse(mockMvc, method, requestBody, statusMatcher, auth, endpoint);
  }

  public static void checkErrorResponse(
      MockMvc mockMvc,
      HttpMethod method,
      String requestBody,
      ResultMatcher statusMatcher,
      RequestPostProcessor auth,
      String endpoint)
      throws Exception {

    mockMvc
        .perform(
            MockMvcRequestBuilders.request(method, URI.create(endpoint))
                .with(auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(statusMatcher);
    //    assertEquals(requestRepository.findAll().size(), 0);
  }

  public static void checkErrorResponse(
      MockMvc mockMvc,
      HttpMethod method,
      Object request,
      ResultMatcher statusMatcher,
      String token,
      String endpoint)
      throws Exception {
    String requestBody = TestUtils.jsonFromRequest(request);
    checkErrorResponse(mockMvc, method, requestBody, statusMatcher, token, endpoint);
  }

  public static void checkErrorResponse(
      MockMvc mockMvc,
      HttpMethod method,
      String requestBody,
      ResultMatcher statusMatcher,
      String token,
      String endpoint)
      throws Exception {

    mockMvc
        .perform(
            MockMvcRequestBuilders.request(method, URI.create(endpoint))
                .header("Authorization", "Bearer %s".formatted(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(statusMatcher);
    //    assertEquals(requestRepository.findAll().size(), 0);
  }

  public static PostCreateDTO createPostDTO(
      String organizationId, String text, PostStatus status, PostType type) {
    return PostCreateDTO.builder()
        .organizationId(organizationId)
        .text(text)
        .status(status)
        .type(type)
        .build();
  }

  public static Post createPost(
      Negotiation negotiation,
      Person author,
      Organization organization,
      String text,
      PostType type) {
    Post post =
        Post.builder()
            .negotiation(negotiation)
            .organization(organization)
            .text(text)
            .type(type)
            .build();
    post.setCreatedBy(author);
    return post;
  }
}
