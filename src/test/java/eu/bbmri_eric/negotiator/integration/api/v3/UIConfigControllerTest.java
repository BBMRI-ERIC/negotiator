package eu.bbmri_eric.negotiator.integration.api.v3;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.bbmri_eric.negotiator.settings.UIParameterRepository;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@IntegrationTest(loadTestData = true)
public class UIConfigControllerTest {

  private MockMvc mvc;
  @Autowired private WebApplicationContext context;
  @Autowired private UIParameterRepository uiParameterRepository;

  @BeforeEach
  public void before() {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @Test
  @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
  void testGetAll_ok() throws Exception {
    mvc.perform(MockMvcRequestBuilders.get("/v3/ui-config"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(jsonPath("$.theme.activeThemeFile").value("default"))
        .andExpect(jsonPath("$.theme.activeLogosFiles").value("default"))
        .andExpect(jsonPath("$.footer.isFooterStatusPageVisible").value(true))
        .andExpect(jsonPath("$.footer.footerHeight").value(50));
  }

  @Test
  void testUpdate_UnauthorizedWhenNoAuth() throws Exception {
    String json =
        """
        {
           "footer": { "footerHeight": 100, "isFooterStatusPageVisible": false },
           "theme": { "activeThemeFile": "color" }
        }
        """;

    mvc.perform(
            MockMvcRequestBuilders.put("/v3/ui-config")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser("researcher")
  void testUpdate_ForbiddenWhenNoAdmin() throws Exception {
    String json =
        """
        {
            "footer": { "footerHeight": 100, "isFooterStatusPageVisible": false },
            "theme": { "activeThemeFile": "color" }
        }
        """;

    mvc.perform(
            MockMvcRequestBuilders.put("/v3/ui-config")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  void testUpdate_ok() throws Exception {
    Assertions.assertEquals(
        uiParameterRepository.findByCategoryAndName("footer", "footerHeight").getValue(), "50");
    Assertions.assertEquals(
        uiParameterRepository
            .findByCategoryAndName("footer", "isFooterStatusPageVisible")
            .getValue(),
        "true");
    Assertions.assertEquals(
        uiParameterRepository.findByCategoryAndName("theme", "activeThemeFile").getValue(),
        "default");

    String json =
        """
         {
            "footer": { "footerHeight": 100, "isFooterStatusPageVisible": false },
            "theme": { "activeThemeFile": "color" }
         }
         """;

    mvc.perform(
            MockMvcRequestBuilders.put("/v3/ui-config")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
        .andExpect(status().isNoContent());

    Assertions.assertEquals(
        "100", uiParameterRepository.findByCategoryAndName("footer", "footerHeight").getValue());
    Assertions.assertEquals(
        "false",
        uiParameterRepository
            .findByCategoryAndName("footer", "isFooterStatusPageVisible")
            .getValue());
    Assertions.assertEquals(
        "color",
        uiParameterRepository.findByCategoryAndName("theme", "activeThemeFile").getValue());
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  void testUpdate_BadRequest_WhenWrongBoolean() throws Exception {
    String json =
        """
        {
           "footer": { "isFooterStatusPageVisible": "wrong" }
        }
        """;

    mvc.perform(
            MockMvcRequestBuilders.put("/v3/ui-config")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.detail")
                .value(
                    "The value 'wrong' for parameter 'isFooterStatusPageVisible' of category 'footer' cannot be applied to type BOOL"));
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  void testUpdate_BadRequest_WhenWrongInteger() throws Exception {
    String json =
        """
        {
           "footer": { "footerHeight": "NaN" }
        }
        """;

    mvc.perform(
            MockMvcRequestBuilders.put("/v3/ui-config")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.detail")
                .value(
                    "The value 'NaN' for parameter 'footerHeight' of category 'footer' cannot be applied to type INT"));
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  void testUpdate_BadRequest_WhenWrongJsonStructure() throws Exception {
    String json =
        """
          [{
             "footer": { "isFooterStatusPageVisible": "NaN" }
          }]
          """;

    mvc.perform(
            MockMvcRequestBuilders.put("/v3/ui-config")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
        .andExpect(status().isBadRequest());

    json =
        """
        {
          "footer": [{ "isFooterStatusPageVisible": "NaN" }]
        }
        """;

    mvc.perform(
            MockMvcRequestBuilders.put("/v3/ui-config")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
        .andExpect(status().isBadRequest());
  }
}
