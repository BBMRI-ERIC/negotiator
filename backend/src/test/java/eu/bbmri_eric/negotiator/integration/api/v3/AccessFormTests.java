package eu.bbmri_eric.negotiator.integration.api.v3;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.negotiator.form.FormElementType;
import eu.bbmri_eric.negotiator.form.dto.AccessFormCreateDTO;
import eu.bbmri_eric.negotiator.form.dto.ElementCreateDTO;
import eu.bbmri_eric.negotiator.form.dto.ElementLinkDTO;
import eu.bbmri_eric.negotiator.form.dto.SectionCreateDTO;
import eu.bbmri_eric.negotiator.form.dto.SectionLinkDTO;
import eu.bbmri_eric.negotiator.form.value_set.ValueSetCreateDTO;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import eu.bbmri_eric.negotiator.util.WithMockNegotiatorUser;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@IntegrationTest(loadTestData = true)
public class AccessFormTests {

  private static final String ACCESS_FORMS_ENDPOINT = "/v3/access-forms";
  private static final String ELEMENTS_ENDPOINT = "/v3/elements";
  private static final String SECTIONS_ENDPOINT = "/v3/sections";
  private static final String VALUE_SETS_ENDPOINT = "/v3/value-sets";
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
  @WithMockUser("researcher")
  void getAllElements_ok() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get(ELEMENTS_ENDPOINT))
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
  @WithMockUser("researcher")
  void getElementById_exists_ok() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get(ELEMENTS_ENDPOINT + "/1"))
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
  @WithMockUser(roles = "ADMIN")
  void createElement_correctPayload_ok() throws Exception {
    ElementCreateDTO createDTO =
        new ElementCreateDTO("test", "test", "test", FormElementType.TEXT, null);
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
  @WithMockUser(roles = "ADMIN")
  void createElement_multiChoiceWithNoValueSet_throwsBadRequest() throws Exception {
    ElementCreateDTO createDTO =
        new ElementCreateDTO("test", "test", "test", FormElementType.MULTIPLE_CHOICE, null);
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(ELEMENTS_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(createDTO)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void updateElement_elementExists_ok() throws Exception {
    ElementCreateDTO createDTO =
        new ElementCreateDTO("test", "test", "test", FormElementType.TEXT, null);
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
  @WithMockUser("researcher")
  void getAllSections_ok() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get(SECTIONS_ENDPOINT))
        .andExpect(content().contentType(MediaTypes.HAL_JSON_VALUE))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.sections").isArray())
        .andExpect(jsonPath("$._embedded.sections[0].id").isNumber())
        .andExpect(jsonPath("$._embedded.sections[0].name").isString())
        .andExpect(jsonPath("$._embedded.sections[0].description").isString());
  }

  @Test
  @WithMockUser("researcher")
  void getSectionById_exists_ok() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get(SECTIONS_ENDPOINT + "/1"))
        .andExpect(content().contentType(MediaTypes.HAL_JSON_VALUE))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._links").isMap())
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.name").isString())
        .andExpect(jsonPath("$.description").isString());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
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
  @WithMockUser(roles = "ADMIN")
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

  @Test
  void getAllAccessForms_someExist_ok() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get(ACCESS_FORMS_ENDPOINT))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.access-forms").isArray())
        .andExpect(jsonPath("$._embedded.access-forms[0].name").isNotEmpty())
        .andExpect(jsonPath("$._embedded.access-forms[0].sections").isArray())
        .andExpect(jsonPath("$._embedded.access-forms[0].sections[0].elements").isArray())
        .andExpect(jsonPath("$._embedded.access-forms[0].sections[0].elements[0].name").isString());
  }

  @Test
  @WithMockNegotiatorUser(id = 101L)
  void getAccessFormForNegotiation_exists_ok() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get("/v3/negotiations/negotiation-1/access-form"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.name").value("BBMRI Template"))
        .andExpect(jsonPath("$.sections[0].name").value("project"))
        .andExpect(jsonPath("$.sections[0].elements[0].label").value("Title"))
        .andExpect(jsonPath("$.sections[0].elements[0].required").value(true))
        .andExpect(jsonPath("$.sections[1].name").value("request"))
        .andExpect(jsonPath("$.sections[2].elements[1].type").value("FILE"))
        .andExpect(jsonPath("$.sections[2].elements[1].required").value(false))
        .andExpect(jsonPath("$._links.self.href").value("http://localhost/v3/access-forms/1"));
  }

  @Test
  void getAccessFormById_exists_ok() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get(ACCESS_FORMS_ENDPOINT + "/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").isNotEmpty())
        .andExpect(jsonPath("$.sections").isArray())
        .andExpect(jsonPath("$.sections[0].elements").isArray())
        .andExpect(jsonPath("$.sections[0].elements[0].name").isString());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void createAccessForm_correctPayload_ok() throws Exception {
    AccessFormCreateDTO requestPayload = new AccessFormCreateDTO("test");
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(ACCESS_FORMS_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(requestPayload)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name", is(requestPayload.getName())));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void linkSectionToAccessForm_bothExist_ok() throws Exception {
    AccessFormCreateDTO requestPayload = new AccessFormCreateDTO("test");
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(ACCESS_FORMS_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(requestPayload)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name", is(requestPayload.getName())))
        .andExpect(jsonPath("$.sections").isEmpty());
    SectionLinkDTO linkDTO = new SectionLinkDTO(1L, 0);
    mockMvc
        .perform(
            MockMvcRequestBuilders.put(ACCESS_FORMS_ENDPOINT + "/100/sections")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(linkDTO)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name", is(requestPayload.getName())))
        .andExpect(jsonPath("$.sections").isArray())
        .andExpect(jsonPath("$.sections[0].id", equalTo(linkDTO.getSectionId().intValue())));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void unlinkSectionFromAccessForm_bothExist_ok() throws Exception {
    AccessFormCreateDTO requestPayload = new AccessFormCreateDTO("test");
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(ACCESS_FORMS_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(requestPayload)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name", is(requestPayload.getName())))
        .andExpect(jsonPath("$.sections").isEmpty());
    SectionLinkDTO linkDTO = new SectionLinkDTO(1L, 0);
    mockMvc
        .perform(
            MockMvcRequestBuilders.put(ACCESS_FORMS_ENDPOINT + "/100/sections")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(linkDTO)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name", is(requestPayload.getName())))
        .andExpect(jsonPath("$.sections").isArray())
        .andExpect(jsonPath("$.sections[0].id", equalTo(linkDTO.getSectionId().intValue())));
    mockMvc
        .perform(MockMvcRequestBuilders.delete(ACCESS_FORMS_ENDPOINT + "/100/sections/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name", is(requestPayload.getName())))
        .andExpect(jsonPath("$.sections").isEmpty());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void linkElementToAccessForm_allExist_ok() throws Exception {
    AccessFormCreateDTO requestPayload = new AccessFormCreateDTO("test");
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(ACCESS_FORMS_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(requestPayload)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name", is(requestPayload.getName())))
        .andExpect(jsonPath("$.sections").isEmpty());
    SectionLinkDTO linkDTO = new SectionLinkDTO(1L, 0);
    mockMvc
        .perform(
            MockMvcRequestBuilders.put(ACCESS_FORMS_ENDPOINT + "/100/sections")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(linkDTO)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name", is(requestPayload.getName())))
        .andExpect(jsonPath("$.sections").isArray())
        .andExpect(jsonPath("$.sections[0].id", equalTo(linkDTO.getSectionId().intValue())))
        .andExpect(jsonPath("$.sections[0].elements").isEmpty());
    ElementLinkDTO elementLinkDTO = new ElementLinkDTO(1L, true, 0);
    mockMvc
        .perform(
            MockMvcRequestBuilders.put(ACCESS_FORMS_ENDPOINT + "/100/sections/1/elements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(elementLinkDTO)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sections[0].elements").isArray())
        .andExpect(
            jsonPath(
                "$.sections[0].elements[0].id", equalTo(elementLinkDTO.getElementId().intValue())));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void unlinkElementFromAccessForm_allExist_ok() throws Exception {
    AccessFormCreateDTO requestPayload = new AccessFormCreateDTO("test");
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(ACCESS_FORMS_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(requestPayload)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name", is(requestPayload.getName())))
        .andExpect(jsonPath("$.sections").isEmpty());
    SectionLinkDTO linkDTO = new SectionLinkDTO(1L, 0);
    mockMvc
        .perform(
            MockMvcRequestBuilders.put(ACCESS_FORMS_ENDPOINT + "/100/sections")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(linkDTO)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name", is(requestPayload.getName())))
        .andExpect(jsonPath("$.sections").isArray())
        .andExpect(jsonPath("$.sections[0].id", equalTo(linkDTO.getSectionId().intValue())))
        .andExpect(jsonPath("$.sections[0].elements").isEmpty());
    ElementLinkDTO elementLinkDTO = new ElementLinkDTO(1L, true, 0);
    mockMvc
        .perform(
            MockMvcRequestBuilders.put(ACCESS_FORMS_ENDPOINT + "/100/sections/1/elements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(elementLinkDTO)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sections[0].elements").isArray())
        .andExpect(
            jsonPath(
                "$.sections[0].elements[0].id", equalTo(elementLinkDTO.getElementId().intValue())));
    mockMvc
        .perform(
            MockMvcRequestBuilders.delete(ACCESS_FORMS_ENDPOINT + "/100/sections/1/elements/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sections[0].elements").isEmpty());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void createValueSet_correctPayload_ok() throws Exception {
    ValueSetCreateDTO createDTO = new ValueSetCreateDTO("test", "test", List.of("idk", "lol"));
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(VALUE_SETS_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(createDTO)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.name", is("test")))
        .andExpect(jsonPath("$.externalDocumentation", is("test")))
        .andExpect(jsonPath("$.availableValues").isArray())
        .andExpect(jsonPath("$._links").isNotEmpty());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void updateValueSet_correctPayload_ok() throws Exception {
    ValueSetCreateDTO createDTO = new ValueSetCreateDTO("test", "test", List.of("idk", "lol"));
    MvcResult mvcResult =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post(VALUE_SETS_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(new ObjectMapper().writeValueAsString(createDTO)))
            .andReturn();
    Long id =
        new ObjectMapper()
            .readTree(mvcResult.getResponse().getContentAsString())
            .get("id")
            .asLong();
    createDTO.setName("updatedTest");
    mockMvc
        .perform(
            MockMvcRequestBuilders.put(VALUE_SETS_ENDPOINT + "/%s".formatted(id))
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(createDTO)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", equalTo(id.intValue())))
        .andExpect(jsonPath("$.name", is(createDTO.getName())));
  }
}
