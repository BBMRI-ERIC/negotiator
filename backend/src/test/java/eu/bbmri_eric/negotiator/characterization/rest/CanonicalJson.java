package eu.bbmri_eric.negotiator.characterization.rest;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Turns a JSON payload into a canonical, human-readable string so that a characterization test can
 * compare bytes on the wire against a committed fixture without depending on incidental ordering.
 *
 * <p>Two orderings in these payloads are incidental and would otherwise make the assertions flaky:
 *
 * <ul>
 *   <li>The lifecycle metadata endpoints collect their members into a {@code HashSet}, so the
 *       {@code _embedded} array arrives in hash order. {@link #canonicalizeCollection} sorts it by
 *       the member's {@code value}.
 *   <li>The lifecycle diagram endpoint builds nested {@code HashMap}s, so object key order is hash
 *       order at every level. Every method here sorts object keys recursively.
 * </ul>
 *
 * <p>Nothing else is normalised: field presence, field values, nesting and repetition all survive
 * into the compared string, which is what the freeze is about.
 */
final class CanonicalJson {

  private static final String FIXTURE_ROOT = "/characterization/rest/";

  private static final ObjectMapper MAPPER =
      JsonMapper.builder().enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS).build();

  private static final ObjectWriter WRITER = MAPPER.writer(prettyPrinter());

  private CanonicalJson() {}

  private static DefaultPrettyPrinter prettyPrinter() {
    DefaultIndenter indenter = new DefaultIndenter("  ", "\n");
    DefaultPrettyPrinter printer = new DefaultPrettyPrinter();
    printer.indentObjectsWith(indenter);
    printer.indentArraysWith(indenter);
    return printer;
  }

  /** Canonicalises a payload whose ordering is entirely a matter of object keys. */
  static String canonicalize(String json) {
    try {
      return WRITER.writeValueAsString(MAPPER.readValue(json, Object.class));
    } catch (IOException e) {
      throw new IllegalArgumentException("Not valid JSON: " + json, e);
    }
  }

  /**
   * Canonicalises a HAL collection payload, sorting {@code _embedded.<rel>} by each member's {@code
   * value} field so that the {@code HashSet} the controller builds cannot make the test flaky.
   */
  @SuppressWarnings("unchecked")
  static String canonicalizeCollection(String json, String rel) {
    try {
      Map<String, Object> root = MAPPER.readValue(json, Map.class);
      Map<String, Object> embedded = (Map<String, Object>) root.get("_embedded");
      Objects.requireNonNull(embedded, "no _embedded in payload: " + json);
      List<Map<String, Object>> members =
          new ArrayList<>((List<Map<String, Object>>) embedded.get(rel));
      Objects.requireNonNull(members, "no _embedded." + rel + " in payload: " + json);
      members.sort(Comparator.comparing(member -> String.valueOf(member.get("value"))));
      embedded.put(rel, members);
      return WRITER.writeValueAsString(root);
    } catch (IOException e) {
      throw new IllegalArgumentException("Not valid JSON: " + json, e);
    }
  }

  /** Reads a committed fixture and normalises its line endings. */
  static String fixture(String name) {
    try (InputStream in = CanonicalJson.class.getResourceAsStream(FIXTURE_ROOT + name)) {
      Objects.requireNonNull(in, "missing fixture " + FIXTURE_ROOT + name);
      return new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n").strip();
    } catch (IOException e) {
      throw new IllegalStateException("could not read fixture " + name, e);
    }
  }
}
