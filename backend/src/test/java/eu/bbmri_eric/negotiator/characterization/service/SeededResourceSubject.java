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
 * <p>{@code negotiation-1} is the only seeded Negotiation that is IN_PROGRESS and carries a
 * Resource in the graph's initial State. Its Resource is {@code biobank:1:collection:1} (row id 4),
 * whose representatives are 109 and 103, while the Negotiation's creator is 108 and neither is an
 * admin. That separates the three Required Authority rules cleanly, which is why a walk of the
 * delivery chain has to change caller between steps.
 *
 * <p><b>The Resource identifier is the Resource's {@code source_id}, never its row id.</b> Both the
 * State lookup ({@code rl.id.resource.sourceId = :resourceId}) and the representative check ({@code
 * resource.getSourceId()}) key on it, and so does the persist listener that writes the new State
 * back. A test that passed "4" would silently find nothing and pass for the wrong reason. The row
 * id appears only in this file's SQL, where the link table's foreign key demands it.
 *
 * <p><b>Placing a Resource in a State.</b> Several Transitions start from States no seeded row is
 * in and that no single caller can reach unaided, so the starting State is written straight onto
 * the link row. That names the State as a string too, and it keeps a table-driven row an
 * independent statement about one Transition rather than a step of one long chain.
 *
 * <p><b>Why the authentication is hand-rolled.</b> {@code authenticateAs} builds the {@code
 * Authentication} the production security filter would - a {@code NegotiatorJwtAuthenticationToken}
 * whose principal wraps the Person, which is what {@code AuthenticatedUserContext} unwraps to an
 * internal id. It is set programmatically rather than by annotation because the caller varies per
 * row of a table-driven test, and an annotation would fix one caller for the whole method.
 *
 * <p><b>Information Requirements and Submissions.</b> The seed carries none of either, so the
 * Information Requirement gate is set up row by row here. Both inserts go through SQL for the same
 * reason the State does: the requirement's {@code for_event} column is an Event name, and writing
 * it as a string keeps the gate's tests free of the Lifecycle enums.
 */
final class SeededResourceSubject {

  static final String NEGOTIATION = "negotiation-1";

  /** The {@code source_id} of Resource row 4, which is what every Lifecycle path keys on. */
  static final String RESOURCE = "biobank:1:collection:1";

  private static final long RESOURCE_ROW_ID = 4L;

  /**
   * A second seeded Resource, used only as "some other Resource" - it is represented by the same
   * Person as the subject and is not linked to {@link #NEGOTIATION} at all.
   */
  static final long ANOTHER_RESOURCE_ROW_ID = 5L;

  static final long ADMIN = 101L;
  static final long REPRESENTATIVE = 109L;
  static final long CREATOR = 108L;

  /** Writes a starting State straight onto the link row, naming it as a string. */
  static void putResourceInState(JdbcTemplate jdbcTemplate, String state) {
    jdbcTemplate.update(
        "update negotiation_resource_link set current_state = ?"
            + " where negotiation_id = ? and resource_id = ?",
        state,
        NEGOTIATION,
        RESOURCE_ROW_ID);
  }

  /** Leaves the link row in place with no recorded State, which is a situation of its own. */
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
   * <p>The Requirement is attached to whichever seeded Access Form comes first: the gate never
   * looks at the form, only at whether a Requirement for the Event exists at all.
   */
  static long requireInformationFor(JdbcTemplate jdbcTemplate, String event) {
    Long accessFormId = jdbcTemplate.queryForObject("select min(id) from access_form", Long.class);
    return jdbcTemplate.queryForObject(
        "insert into information_requirement (required_access_form_id, for_event)"
            + " values (?, ?) returning id",
        Long.class,
        accessFormId,
        event);
  }

  /** Records an Information Submission against {@code requirementId} for the subject Resource. */
  static void submitInformationFor(JdbcTemplate jdbcTemplate, long requirementId) {
    submitInformationFor(jdbcTemplate, requirementId, RESOURCE_ROW_ID);
  }

  /** Records an Information Submission for a nominated Resource of {@link #NEGOTIATION}. */
  static void submitInformationFor(
      JdbcTemplate jdbcTemplate, long requirementId, long resourceRowId) {
    jdbcTemplate.update(
        "insert into information_submission (requirement_id, resource_id, negotiation_id, payload)"
            + " values (?, ?, ?, cast(? as json))",
        requirementId,
        resourceRowId,
        NEGOTIATION,
        "{}");
  }

  /** Authenticates as a seeded Person, with the admin authority only for {@link #ADMIN}. */
  static void authenticateAs(long personId) {
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
