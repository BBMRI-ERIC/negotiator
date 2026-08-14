package eu.bbmri_eric.negotiator.characterization.rest;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * The suite's one reader of committed artifacts, and the one place that knows how to compare a JSON
 * payload without depending on incidental ordering.
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
 *
 * <p>{@link #artifact}, {@link #namesIn} and {@link #publishedValues} serve the other half of the
 * suite: the classes that bind a hand-written graph table to the mechanical dump and to the
 * published metadata. They read the same committed files this package compares against, so they
 * live here rather than being written out again per binding test.
 */
public final class CanonicalJson {

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

  /**
   * Reads a committed artifact anywhere on the test classpath as a JSON tree - the mechanical graph
   * dumps under {@code lifecycle/} as well as the metadata fixtures under {@code
   * characterization/rest/}.
   *
   * @throws IllegalStateException if the artifact is not on the classpath, which must never be
   *     mistaken for "the binding holds"
   */
  public static JsonNode artifact(String classpathResource) {
    try (InputStream in =
        CanonicalJson.class.getClassLoader().getResourceAsStream(classpathResource)) {
      if (in == null) {
        throw new IllegalStateException(
            "Committed artifact "
                + classpathResource
                + " is missing from the test classpath. The pinned graph has nothing to be bound to,"
                + " and a table nothing binds must never be treated as a statement about the"
                + " system.");
      }
      return MAPPER.readTree(new String(in.readAllBytes(), StandardCharsets.UTF_8));
    } catch (IOException e) {
      throw new IllegalStateException("Could not read committed artifact " + classpathResource, e);
    }
  }

  /** The text values of a JSON array of names, as a set. */
  public static Set<String> namesIn(JsonNode array) {
    return StreamSupport.stream(array.spliterator(), false)
        .map(JsonNode::asText)
        .collect(Collectors.toUnmodifiableSet());
  }

  /**
   * The user-facing {@code label} of every member of a committed HAL collection, keyed by its
   * {@code value}.
   *
   * <p>Labels are data the redesign carries forward rather than names it deletes, and they reach
   * users through more than the metadata endpoint - a notification body is built out of two of
   * them. Reading them from the committed artifact rather than transcribing them keeps that one
   * statement of the labels, so a label edit shows up in one place instead of two.
   */
  public static Map<String, String> publishedLabels(String classpathResource, String rel) {
    Map<String, String> labels = new LinkedHashMap<>();
    artifact(classpathResource)
        .get("_embedded")
        .get(rel)
        .forEach(member -> labels.put(member.get("value").asText(), member.get("label").asText()));
    return labels;
  }

  /**
   * The {@code value} of every member of a committed HAL collection - the shape both lifecycle
   * metadata endpoints publish their States and Events in.
   */
  public static Set<String> publishedValues(String classpathResource, String rel) {
    return StreamSupport.stream(
            artifact(classpathResource).get("_embedded").get(rel).spliterator(), false)
        .map(member -> member.get("value").asText())
        .collect(Collectors.toUnmodifiableSet());
  }
}
