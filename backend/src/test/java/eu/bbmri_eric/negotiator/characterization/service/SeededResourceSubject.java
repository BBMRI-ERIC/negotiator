package eu.bbmri_eric.negotiator.characterization.service;

import eu.bbmri_eric.negotiator.common.configuration.security.oauth2.NegotiatorJwtAuthenticationToken;
import eu.bbmri_eric.negotiator.user.Person;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * The seeded Resource both Resource characterization classes work on, the three seeded callers that
 * satisfy its Transitions' Required Authority rules, and the SQL that places it in a State.
 *
 * <p>{@code negotiation-1} is the only seeded Negotiation that is IN_PROGRESS with a Resource in
 * the graph's initial State. That Resource is {@code biobank:1:collection:1} (row id 4), represented
 * by 109 and 103; the creator is 108, and none of them is an admin. Each Required Authority value
 * therefore maps to a different caller, so a walk of the delivery chain changes caller between
 * steps.
 *
 * <p>A Resource is identified by its {@code source_id}, never its row id. The State lookup, the
 * representative check and the persist listener all key on it, so a test that passed "4" would
 * find nothing and pass for the wrong reason. The row id appears only in this file's SQL, where the
 * link table's foreign key needs it.
 *
 * <p>Tests write the starting State straight onto the link row, because several Transitions start
 * from States no seeded row is in and no single caller can reach. Each table row then tests one
 * Transition on its own instead of a step of a long chain.
 *
 * <p>{@code authenticateAs} builds the {@code Authentication} the production security filter would:
 * a {@code NegotiatorJwtAuthenticationToken} whose principal wraps the Person, which {@code
 * AuthenticatedUserContext} unwraps to an internal id. It is set in code rather than by annotation
 * because the caller varies per table row.
 *
 * <p>The seed has no Information Requirements or Submissions, so tests insert them here through
 * SQL. The requirement's {@code for_event} column holds an Event name, so writing it as a string
 * keeps the tests free of the Lifecycle enums.
 *
 * <p>The members the {@code delta} package uses are public; everything else stays package private.
 */
public final class SeededResourceSubject {

  public static final String NEGOTIATION = "negotiation-1";

  /** The {@code source_id} of Resource row 4, which is what every Lifecycle path keys on. */
  public static final String RESOURCE = "biobank:1:collection:1";

  /**
   * The database row id of {@link #RESOURCE}. Outside this file only the Override path needs it,
   * because it is the one place that identifies a Resource by row id.
   */
  static final long RESOURCE_ROW_ID = 4L;

  /**
   * A second seeded Resource, represented by the same Person as the subject and not linked to
   * {@link #NEGOTIATION} by the seed. Tests use it as "some other Resource" and, after {@link
   * #linkResource}, as a second Resource so "every Resource is terminal" covers more than one.
   */
  static final long ANOTHER_RESOURCE_ROW_ID = 5L;

  /** The {@code source_id} of {@link #ANOTHER_RESOURCE_ROW_ID}. */
  static final String ANOTHER_RESOURCE = "biobank:1:collection:2";

  public static final long ADMIN = 101L;
  static final long REPRESENTATIVE = 109L;
  public static final long CREATOR = 108L;

  /**
   * The subject Resource's second representative, so "the representatives of a Resource" is a set
   * and not a single id. Person 103 represents nothing else, so a notification to them can only come
   * through this Resource.
   */
  static final long SECOND_REPRESENTATIVE = 103L;

  /** Writes a starting State straight onto the link row, naming it as a string. */
  static void putResourceInState(JdbcTemplate jdbcTemplate, String state) {
    jdbcTemplate.update(
        "update negotiation_resource_link set current_state = ?"
            + " where negotiation_id = ? and resource_id = ?",
        state,
        NEGOTIATION,
        RESOURCE_ROW_ID);
  }

  /**
   * Attaches a further Resource to {@link #NEGOTIATION} in a nominated State.
   *
   * <p>Uses SQL rather than the governance service, because that service publishes its own state
   * change and, on a running Negotiation, sets off the handlers the test is about to observe.
   */
  static void linkResource(JdbcTemplate jdbcTemplate, long resourceRowId, String state) {
    jdbcTemplate.update(
        "insert into negotiation_resource_link (negotiation_id, resource_id, current_state)"
            + " values (?, ?, ?)",
        NEGOTIATION,
        resourceRowId,
        state);
  }

  /** Leaves the link row in place with no recorded State. */
  static void clearResourceState(JdbcTemplate jdbcTemplate) {
    jdbcTemplate.update(
        "update negotiation_resource_link set current_state = null"
            + " where negotiation_id = ? and resource_id = ?",
        NEGOTIATION,
        RESOURCE_ROW_ID);
  }

  /** Places the parent Negotiation in a State, for walking the IN_PROGRESS gate. */
  static void putNegotiationInState(JdbcTemplate jdbcTemplate, String state) {
    jdbcTemplate.update(
        "update negotiation set current_state = ? where id = ?", state, NEGOTIATION);
  }

  /**
   * Declares that {@code event} requires a form to have been filled in, and returns the new
   * Information Requirement's id.
   *
   * <p>The Requirement uses the first seeded Access Form, because the gate checks only that a
   * Requirement for the Event exists, never the form.
   */
  public static long requireInformationFor(JdbcTemplate jdbcTemplate, String event) {
    Long accessFormId = jdbcTemplate.queryForObject("select min(id) from access_form", Long.class);
    return jdbcTemplate.queryForObject(
        "insert into information_requirement (required_access_form_id, for_event)"
            + " values (?, ?) returning id",
        Long.class,
        accessFormId,
        event);
  }

  /**
   * Records an Information Submission against {@code requirementId} for the subject Resource, and
   * returns the new row's id, which the assembler builds its per-row rel names from.
   */
  public static long submitInformationFor(JdbcTemplate jdbcTemplate, long requirementId) {
    return submitInformationFor(jdbcTemplate, requirementId, RESOURCE_ROW_ID);
  }

  /** Records an Information Submission for a nominated Resource of {@link #NEGOTIATION}. */
  static long submitInformationFor(
      JdbcTemplate jdbcTemplate, long requirementId, long resourceRowId) {
    return jdbcTemplate.queryForObject(
        "insert into information_submission (requirement_id, resource_id, negotiation_id, payload)"
            + " values (?, ?, ?, cast(? as json)) returning id",
        Long.class,
        requirementId,
        resourceRowId,
        NEGOTIATION,
        "{}");
  }

  /** Authenticates as a seeded Person, with the admin authority only for {@link #ADMIN}. */
  public static void authenticateAs(long personId) {
    authenticateAs(personId, personId == ADMIN ? List.of("ROLE_ADMIN") : List.of());
  }

  static void authenticateAs(long personId, List<String> authorities) {
    Person principal = Person.builder().id(personId).name("caller-" + personId).build();
    Collection<GrantedAuthority> granted = new ArrayList<>();
    authorities.forEach(authority -> granted.add(new SimpleGrantedAuthority(authority)));
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(new NegotiatorJwtAuthenticationToken(principal, testJwt(), granted));
    SecurityContextHolder.setContext(context);
  }

  private static Jwt testJwt() {
    HashMap<String, Object> headers = new HashMap<>();
    headers.put("typ", "JWT");
    HashMap<String, Object> claims = new HashMap<>();
    claims.put("sub", "characterization");
    return new Jwt(
        "testToken", Instant.now(), Instant.now().plus(3L, ChronoUnit.HOURS), headers, claims);
  }

  private SeededResourceSubject() {}
}
