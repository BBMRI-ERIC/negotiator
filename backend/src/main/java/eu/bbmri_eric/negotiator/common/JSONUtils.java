package eu.bbmri_eric.negotiator.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;

public final class JSONUtils {
  private JSONUtils() {}

  public static boolean isJSONValid(String jsonInString) {
    try {
      final ObjectMapper mapper = new ObjectMapper();
      mapper.readTree(jsonInString);
      return true;
    } catch (IOException e) {
      return false;
    }
  }

  public static String toJSON(String jsonInString) {
    try {
      final ObjectMapper mapper = new ObjectMapper();
      return mapper.writeValueAsString(jsonInString);
    } catch (IOException e) {
      throw new IllegalArgumentException("Failed to convert to JSON", e);
    }
  }
}
