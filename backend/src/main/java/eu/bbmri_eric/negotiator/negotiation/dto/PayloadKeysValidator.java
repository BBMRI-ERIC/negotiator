package eu.bbmri_eric.negotiator.negotiation.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Iterator;
import java.util.Map;

public class PayloadKeysValidator implements ConstraintValidator<ValidPayloadKeys, JsonNode> {

  @Override
  public boolean isValid(JsonNode payload, ConstraintValidatorContext context) {
    return payload == null || hasOnlySafeKeys(payload);
  }

  private boolean hasOnlySafeKeys(JsonNode node) {
    if (node.isObject()) {
      Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
      while (fields.hasNext()) {
        Map.Entry<String, JsonNode> field = fields.next();
        if (containsForbiddenCharacter(field.getKey()) || !hasOnlySafeKeys(field.getValue())) {
          return false;
        }
      }
    } else if (node.isArray()) {
      for (JsonNode element : node) {
        if (!hasOnlySafeKeys(element)) {
          return false;
        }
      }
    }
    return true;
  }

  private boolean containsForbiddenCharacter(String key) {
    return key.indexOf('<') >= 0 || key.indexOf('>') >= 0 || key.indexOf('&') >= 0;
  }
}
