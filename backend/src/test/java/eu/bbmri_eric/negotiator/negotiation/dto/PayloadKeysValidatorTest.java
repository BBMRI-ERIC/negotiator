package eu.bbmri_eric.negotiator.negotiation.dto;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("PayloadKeysValidator Tests")
class PayloadKeysValidatorTest {

  private final PayloadKeysValidator validator = new PayloadKeysValidator();
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  @DisplayName("Should accept null payload")
  void shouldAcceptNullPayload() {
    assertTrue(validator.isValid(null, null));
  }

  @Test
  @DisplayName("Should accept payload with ordinary field names")
  void shouldAcceptOrdinaryFieldNames() throws Exception {
    JsonNode payload =
        objectMapper.readTree(
            "{\"access-form\": {\"sample-type\": \"blood\", \"quantity\": 5}}");

    assertTrue(validator.isValid(payload, null));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "<img src=x onerror=alert(1)>",
        "&lt;img src=x onerror=alert(1)&gt;",
        "sample&type",
        "sample<type",
        "sample>type"
      })
  @DisplayName("Should reject a top-level key containing '<', '>' or '&'")
  void shouldRejectTopLevelForbiddenKey(String maliciousKey) throws Exception {
    JsonNode payload =
        objectMapper.createObjectNode().set(maliciousKey, objectMapper.createObjectNode());

    assertFalse(validator.isValid(payload, null));
  }

  @Test
  @DisplayName("Should reject a nested key containing forbidden characters")
  void shouldRejectNestedForbiddenKey() throws Exception {
    JsonNode payload =
        objectMapper.readTree(
            "{\"access-form\": {\"&lt;img src=x onerror=alert(1)&gt;\": \"blood\"}}");

    assertFalse(validator.isValid(payload, null));
  }

  @Test
  @DisplayName("Should reject a forbidden key nested inside an array")
  void shouldRejectForbiddenKeyInsideArray() throws Exception {
    JsonNode payload =
        objectMapper.readTree(
            "{\"access-form\": [{\"sample-type\": \"blood\"}, {\"<script>\": \"x\"}]}");

    assertFalse(validator.isValid(payload, null));
  }

  @Test
  @DisplayName("Should accept free-text values containing '<', '>' or '&'")
  void shouldAcceptForbiddenCharactersInValues() throws Exception {
    JsonNode payload =
        objectMapper.readTree(
            "{\"access-form\": {\"justification\": \"age < 18 & consent required\"}}");

    assertTrue(validator.isValid(payload, null));
  }
}
