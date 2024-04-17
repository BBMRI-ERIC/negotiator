package eu.bbmri_eric.negotiator.integration.api.v3;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.negotiator.NegotiatorApplication;
import eu.bbmri_eric.negotiator.dto.access_form.ElementCreateDTO;
import eu.bbmri_eric.negotiator.dto.access_form.SectionCreateDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(classes = NegotiatorApplication.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class AccessFormTests {

  private static final String ENDPOINT = "/v3/access-criteria";
  private static final String ELEMENTS_ENDPOINT = "/v3/elements";
  private static final String SECTIONS_ENDPOINT = "/v3/sections";
  private static final String CORRECT_TOKEN_VALUE = "researcher";
  private static final String FORBIDDEN_TOKEN_VALUE = "unknown";
  private static final String UNAUTHORIZED_TOKEN_VALUE = "unauthorized";

  private MockMvc mockMvc;

  @Autowired private WebApplicationContext context;
  @Autowired private ModelMapper modelMapper;

  @BeforeEach
  public void before() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @Test
  public void testGet_Unauthorized_whenWrongAuth() throws Exception {
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.GET,
        "",
        status().isUnauthorized(),
        httpBasic("researcher", "wrong_pass"),
        ENDPOINT);
  }

  @Test
  public void testGet_BadRequest_whenMissingResourceId() throws Exception {
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.GET,
        "",
        status().isBadRequest(),
        httpBasic("researcher", "researcher"),
        ENDPOINT);
  }

  @Test
  public void testGet_NotFound_whenResourceIdIsNotExistent() throws Exception {
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.GET,
        "",
        status().isNotFound(),
        httpBasic("researcher", "researcher"),
        "%s?resourceId=UNKNOWN".formatted(ENDPOINT));
  }

  @Test
  public void testGet_Ok() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .param("resourceId", "biobank:1:collection:1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sections[0].elements").isArray())
        .andExpect(jsonPath("$.sections[0].name", is("project")))
        .andExpect(jsonPath("$.sections[0].elements[0].name", is("title")))
        .andExpect(jsonPath("$.sections[0].elements[0].required", is(true)))
        .andExpect(jsonPath("$.sections[0].elements[1].name", is("description")))
        .andExpect(jsonPath("$.sections[1].name", is("request")))
        .andExpect(jsonPath("$.sections[1].elements[0].name", is("description")))
        .andExpect(jsonPath("$.sections[2].name", is("ethics-vote")));
  }

  @Test
  void getAllElements_ok() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(ELEMENTS_ENDPOINT).contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().contentType(MediaTypes.HAL_JSON_VALUE))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.elements").isArray())
        .andExpect(jsonPath("$._embedded.elements[0].id").isNumber())
        .andExpect(jsonPath("$._embedded.elements[0].name").isString())
        .andExpect(jsonPath("$._embedded.elements[0].type").isString())
        .andExpect(jsonPath("$._embedded.elements[0].description").isString())
        .andExpect(jsonPath("$._embedded.elements[0].label").isString());
  }

  @Test
  void getElementById_exists_ok() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(ELEMENTS_ENDPOINT + "/1")
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().contentType(MediaTypes.HAL_JSON_VALUE))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._links").isMap())
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.name").isString())
        .andExpect(jsonPath("$.type").isString())
        .andExpect(jsonPath("$.description").isString())
        .andExpect(jsonPath("$.label").isString());
  }

  @Test
  void createElement_correctPayload_ok() throws Exception {
    ElementCreateDTO createDTO = new ElementCreateDTO("test", "test", "test", "test");
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(ELEMENTS_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(createDTO)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.name", is("test")))
        .andExpect(jsonPath("$._links").isNotEmpty());
  }

  @Test
  void updateElement_elementExists_ok() throws Exception {
    ElementCreateDTO createDTO = new ElementCreateDTO("test", "test", "test", "test");
    MvcResult mvcResult =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post(ELEMENTS_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(new ObjectMapper().writeValueAsString(createDTO)))
            .andExpect(status().isCreated())
            .andReturn();
    Long id =
        new ObjectMapper()
            .readTree(mvcResult.getResponse().getContentAsString())
            .get("id")
            .asLong();
    createDTO.setName("updatedTest");
    mockMvc
        .perform(
            MockMvcRequestBuilders.put(ELEMENTS_ENDPOINT + "/%s".formatted(id))
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(createDTO)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", equalTo(id.intValue())))
        .andExpect(jsonPath("$.name", is(createDTO.getName())));
  }

  @Test
  void getAllSections_ok() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(SECTIONS_ENDPOINT).contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().contentType(MediaTypes.HAL_JSON_VALUE))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.sections").isArray())
        .andExpect(jsonPath("$._embedded.sections[0].id").isNumber())
        .andExpect(jsonPath("$._embedded.sections[0].name").isString())
        .andExpect(jsonPath("$._embedded.sections[0].description").isString());
  }

  @Test
  void getSectionById_exists_ok() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(SECTIONS_ENDPOINT + "/1")
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().contentType(MediaTypes.HAL_JSON_VALUE))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._links").isMap())
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.name").isString())
        .andExpect(jsonPath("$.description").isString());
  }

  @Test
  void createSection_correctPayload_ok() throws Exception {
    SectionCreateDTO createDTO = new SectionCreateDTO("test", "test", "test");
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(SECTIONS_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(createDTO)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.name", is("test")))
        .andExpect(jsonPath("$._links").isNotEmpty());
  }

  @Test
  void updateSection_sectionExists_ok() throws Exception {
    SectionCreateDTO createDTO = new SectionCreateDTO("test", "test", "test");
    MvcResult mvcResult =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post(SECTIONS_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(new ObjectMapper().writeValueAsString(createDTO)))
            .andExpect(status().isCreated())
            .andReturn();
    Long id =
        new ObjectMapper()
            .readTree(mvcResult.getResponse().getContentAsString())
            .get("id")
            .asLong();
    createDTO.setName("updatedTest");
    mockMvc
        .perform(
            MockMvcRequestBuilders.put(SECTIONS_ENDPOINT + "/%s".formatted(id))
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(createDTO)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", equalTo(id.intValue())))
        .andExpect(jsonPath("$.name", is(createDTO.getName())));
  }
}
