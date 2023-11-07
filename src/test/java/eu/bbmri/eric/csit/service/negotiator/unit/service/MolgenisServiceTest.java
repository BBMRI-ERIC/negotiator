package eu.bbmri.eric.csit.service.negotiator.unit.service;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import eu.bbmri.eric.csit.service.negotiator.dto.MolgenisCollection;
import eu.bbmri.eric.csit.service.negotiator.service.MolgenisServiceImplementation;
import java.util.Optional;
import lombok.NonNull;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

@WireMockTest(httpPort = 8080)
public class MolgenisServiceTest {

  @NonNull
  private static ObjectNode creatJsonBody(String biobankId, String collectionId) {
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode actualObj = mapper.createObjectNode();
    ObjectNode biobank = mapper.createObjectNode();
    biobank.put("_href", "/api/v2/eu_bbmri_eric_biobanks/bbmri-eric:ID:BB");
    biobank.put("id", biobankId);
    biobank.put("name", "Biobank 1");
    actualObj.put("id", collectionId);
    actualObj.put("name", "Collection 1");
    actualObj.put("not_relevant_string", "not_relevant_value");
    actualObj.putIfAbsent("biobank", biobank);
    return actualObj;
  }

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
    String collectionId = "bbmri:eric:collection:1";
    String biobankId = "bbmri-eric:ID:BB";
    String baseUrl = "http://localhost:8080/directory";
    ObjectNode actualObj = creatJsonBody(biobankId, collectionId);
    stubFor(
        get(urlEqualTo("/directory/api/v2/eu_bbmri_eric_collections/bbmri:eric:collection:1"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withJsonBody(actualObj)));
    Optional<MolgenisCollection> molgenisCollection =
        new MolgenisServiceImplementation(WebClient.create(baseUrl))
            .findCollectionById(collectionId);
    assertTrue(molgenisCollection.isPresent());
    assertEquals("Collection 1", molgenisCollection.get().getName());
    assertEquals(collectionId, molgenisCollection.get().getId());
    assertEquals(biobankId, molgenisCollection.get().getBiobank().getId());
  }

  @Test
  void findByCollectionId_invalidId_empty() {
    String collectionId = "bbmri:eric:collection:1";
    String baseUrl = "http://localhost:8080/directory";
    stubFor(
        get(urlEqualTo("/directory/api/v2/eu_bbmri_eric_collections/bbmri:eric:collection:1"))
            .willReturn(aResponse().withStatus(404)));
    assertEquals(
        Optional.empty(),
        new MolgenisServiceImplementation(WebClient.create(baseUrl))
            .findCollectionById(collectionId));
  }

  @Test
  void findByBiobankId_validId_Ok() {
    String baseUrl = "http://localhost:8080/directory";
    String biobankId = "bbmri-eric:ID:BB";
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode biobank = mapper.createObjectNode();
    biobank.put("_href", "/api/v2/eu_bbmri_eric_biobanks/bbmri-eric:ID:BB");
    biobank.put("id", biobankId);
    biobank.put("name", "Biobank 1");
    stubFor(
        get(urlEqualTo("/directory/api/v2/eu_bbmri_eric_biobanks/bbmri-eric:ID:BB"))
            .willReturn(
                aResponse().withHeader("Content-Type", "application/json").withJsonBody(biobank)));
    assertEquals(
        biobankId,
        new MolgenisServiceImplementation(WebClient.create(baseUrl))
            .findBiobankById(biobankId)
            .get()
            .getId());
  }

  @Test
  void findByCollectionId_notReachableMolgenis_returnsEmpty() {
    assertEquals(
        Optional.empty(),
        new MolgenisServiceImplementation(WebClient.create(""))
            .findCollectionById("does not matter"));
  }
}
