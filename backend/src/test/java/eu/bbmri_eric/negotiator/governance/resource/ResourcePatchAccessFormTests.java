package eu.bbmri_eric.negotiator.governance.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.bbmri_eric.negotiator.form.AccessForm;
import eu.bbmri_eric.negotiator.form.repository.AccessFormRepository;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Regression coverage for PR #1127 ("default access form selection"), which made accessFormId
 * optional on resource creation and resolves an omitted one to the organization's most common form,
 * falling back to form 1.
 *
 * <p>The reported symptom is that an external service PATCHing a resource without sending an access
 * form has its non-default access form replaced by the default one. These tests pin down what the
 * PATCH endpoint actually does to an already-assigned, non-default access form: leaving it untouched
 * is the expected behaviour of a partial update.
 *
 * <p>Resource 7 belongs to organization 6, whose other resources (8, 9, 10) all use access form 1.
 * So if the endpoint were to re-resolve a default, it would land on form 1 — distinguishable from
 * the non-default form 201 assigned here.
 */
@IntegrationTest(loadTestData = true)
public class ResourcePatchAccessFormTests {

  private static final String RESOURCE_ENDPOINT = "/v3/resources/%s";
  private static final long RESOURCE_ID = 7L;
  private static final long DEFAULT_ACCESS_FORM_ID = 1L;
  private static final long NON_DEFAULT_ACCESS_FORM_ID = 201L;

  @Autowired private WebApplicationContext context;
  @Autowired private ResourceRepository resourceRepository;
  @Autowired private AccessFormRepository accessFormRepository;

  private MockMvc mockMvc;

  @BeforeEach
  public void before() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    assignAccessForm(NON_DEFAULT_ACCESS_FORM_ID);
    assertEquals(
        NON_DEFAULT_ACCESS_FORM_ID,
        currentAccessFormId(),
        "precondition: the resource under test starts on a non-default access form");
  }

  @AfterEach
  public void after() {
    assignAccessForm(DEFAULT_ACCESS_FORM_ID);
  }

  private void assignAccessForm(long accessFormId) {
    AccessForm accessForm = accessFormRepository.findById(accessFormId).orElseThrow();
    Resource resource = resourceRepository.findById(RESOURCE_ID).orElseThrow();
    resource.setAccessForm(accessForm);
    resourceRepository.saveAndFlush(resource);
  }

  private Long currentAccessFormId() {
    Resource resource = resourceRepository.findById(RESOURCE_ID).orElseThrow();
    assertNotNull(resource.getAccessForm(), "the resource lost its access form entirely");
    return resource.getAccessForm().getId();
  }

  private void patch(String body) throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.patch(RESOURCE_ENDPOINT.formatted(RESOURCE_ID))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isCreated());
  }

  /** The shape an external service sends: a partial body that never mentions the access form. */
  @Test
  @WithUserDetails("admin")
  void updateResource_bodyWithoutAccessFormId_keepsAssignedAccessForm() throws Exception {
    patch("{\"name\":\"Renamed by an external service\"}");

    assertEquals(
        NON_DEFAULT_ACCESS_FORM_ID,
        currentAccessFormId(),
        "a PATCH that does not mention the access form must not reassign it");
  }

  /** The same call from a client that serialises unset fields as explicit nulls. */
  @Test
  @WithUserDetails("admin")
  void updateResource_bodyWithExplicitNullAccessFormId_keepsAssignedAccessForm() throws Exception {
    patch("{\"name\":\"Renamed by an external service\",\"accessFormId\":null}");

    assertEquals(
        NON_DEFAULT_ACCESS_FORM_ID,
        currentAccessFormId(),
        "a PATCH carrying accessFormId: null must not reassign the access form");
  }

  /** A full-looking body, still without the access form — the way a naive read-modify-write looks. */
  @Test
  @WithUserDetails("admin")
  void updateResource_fullBodyWithoutAccessFormId_keepsAssignedAccessForm() throws Exception {
    patch(
        """
        {
          "name": "Renamed by an external service",
          "description": "Rewritten description",
          "contactEmail": "service@test.org",
          "uri": "https://service.test.org",
          "withdrawn": false
        }
        """);

    assertEquals(
        NON_DEFAULT_ACCESS_FORM_ID,
        currentAccessFormId(),
        "a full PATCH body that omits the access form must not reassign it");
  }
}
