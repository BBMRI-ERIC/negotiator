package eu.bbmri.eric.csit.service.negotiator.unit.service;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import eu.bbmri.eric.csit.service.negotiator.service.MolgenisServiceImplementation;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

@WireMockTest(httpPort = 8080)
public class MolgenisServiceTest {

  @Test
  void isReachable_nullUrl_False() {
    assertThrows(NullPointerException.class, () -> new MolgenisServiceImplementation(null));
  }

  @Test
  void isReachable_emptyString_False() {
    assertFalse(new MolgenisServiceImplementation(WebClient.create("")).isReachable());
  }

  @Test
  void isReachable_correctUrl_True() {
    stubFor(
        get(urlEqualTo("/directory"))
            .willReturn(aResponse().withBody("Directory is Up and Running!")));
    assertTrue(
        new MolgenisServiceImplementation(WebClient.create("http://localhost:8080/directory"))
            .isReachable());
  }

  @Test
  void findByCollectionId_null_throwsNullPointer() {
    assertThrows(
        NullPointerException.class,
        () ->
            new MolgenisServiceImplementation(WebClient.create("http://localhost:8080/directory"))
                .findCollectionById(null));
  }

  @Test
  void findByCollectionId_valid_Ok() {
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode actualObj = mapper.createObjectNode();
    actualObj.put("id", "bbmri:eric:collection:1");
    actualObj.put("name", "Collection 1");
    actualObj.put("not_relevant_string", "not_relevant_value");
    stubFor(
        get(urlEqualTo("/directory/api/v2/collections/bbmri:eric:collection:1"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withJsonBody(actualObj)));
    assertTrue(
        new MolgenisServiceImplementation(WebClient.create("http://localhost:8080/directory"))
            .findCollectionById("bbmri:eric:collection:1")
            .isPresent());
    assertEquals(
        "Collection 1",
        new MolgenisServiceImplementation(WebClient.create("http://localhost:8080/directory"))
            .findCollectionById("bbmri:eric:collection:1")
            .get()
            .getName());
    assertEquals(
        "bbmri:eric:collection:1",
        new MolgenisServiceImplementation(WebClient.create("http://localhost:8080/directory"))
            .findCollectionById("bbmri:eric:collection:1")
            .get()
            .getId());
  }
}
