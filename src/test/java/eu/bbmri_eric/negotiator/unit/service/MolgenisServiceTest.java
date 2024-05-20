package eu.bbmri_eric.negotiator.unit.service;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import eu.bbmri_eric.negotiator.dto.MolgenisBiobank;
import eu.bbmri_eric.negotiator.dto.MolgenisCollection;
import eu.bbmri_eric.negotiator.service.MolgenisServiceImplementation;
import java.util.Arrays;
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

  @Test
  void findAllBiobanks_Ok() {
    String baseUrl = "http://localhost:8080/directory";
    String biobankId = "bbmri-eric:ID:BB";
    String biobank2Id = "bbmri-eric:ID:BB_2";
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode biobank = mapper.createObjectNode();
    ArrayNode biobanks = mapper.createArrayNode();
    biobank.put("_href", "/api/v2/eu_bbmri_eric_biobanks/bbmri-eric:ID:BB");
    biobank.put("id", biobankId);
    biobank.put("name", "Biobank 1");
    ObjectNode biobank2 = mapper.createObjectNode();
    biobank2.put("_href", "/api/v2/eu_bbmri_eric_biobanks/bbmri-eric:ID:BB_2");
    biobank2.put("id", biobank2Id);
    biobank2.put("name", "Biobank 2");
    biobanks.addAll(Arrays.asList(biobank, biobank2));
    JsonNode result = mapper.createObjectNode().set("items", biobanks);
    stubFor(
        get(urlEqualTo(
                "/directory/api/v2/eu_bbmri_eric_biobanks?num=10000&q=withdrawn%3D%3Dfalse&attrs=id,name"))
            .willReturn(
                aResponse().withHeader("Content-Type", "application/json").withJsonBody(result)));
    assertEquals(
        2, new MolgenisServiceImplementation(WebClient.create(baseUrl)).findAllBiobanks().size());
  }

  @Test
  void findAllCollectionsByBiobankId_Ok() {
    String baseUrl = "http://localhost:8080/directory";
    String biobankId = "bbmri-eric:ID:BB";
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode biobank = mapper.createObjectNode();
    String biobankHref = "/api/v2/eu_bbmri_eric_biobanks/bbmri-eric:ID:BB";
    String biobankName = "Biobank 1";
    biobank.put("_href", biobankHref);
    biobank.put("id", biobankId);
    biobank.put("name", biobankName);
    MolgenisBiobank molgenisBiobank = new MolgenisBiobank(biobankId, biobankName, biobankHref);
    String collection1Id = "bbmri-eric:ID:BB_collection1";
    String collection2Id = "bbmri-eric:ID:BB_collection2";
    ObjectNode collection1 = mapper.createObjectNode();
    collection1.put("_href", "/api/v2/eu_bbmri_eric_collections/bbmri-eric:ID:BB_collection1");
    collection1.put("id", collection1Id);
    collection1.put("name", "Collection 1");
    collection1.put("description", "Collection 1");
    ObjectNode collection2 = mapper.createObjectNode();
    collection2.put("_href", "/api/v2/eu_bbmri_eric_collections/bbmri-eric:ID:BB_collection2");
    collection2.put("id", collection2Id);
    collection2.put("name", "Collection 2");
    collection2.put("description", "Collection 2");
    ArrayNode collections = mapper.createArrayNode();
    collections.addAll(Arrays.asList(collection1, collection2));
    JsonNode result = mapper.createObjectNode().set("items", collections);
    stubFor(
        get(urlEqualTo(
                String.format(
                    "/directory/api/v2/eu_bbmri_eric_collections?q=biobank%s%s&num=10000&attrs=id,name,description",
                    "%3D%3D", biobankId)))
            .willReturn(
                aResponse().withHeader("Content-Type", "application/json").withJsonBody(result)));
    assertEquals(
        2,
        new MolgenisServiceImplementation(WebClient.create(baseUrl))
            .findAllCollectionsByBiobankId(molgenisBiobank)
            .size());
  }
}
