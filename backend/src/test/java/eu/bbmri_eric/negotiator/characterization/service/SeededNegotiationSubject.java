package eu.bbmri_eric.negotiator.characterization.service;

import eu.bbmri_eric.negotiator.characterization.service.NegotiationGraphV1.PostFlags;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The seeded Negotiations the post-effect classes drive, and the SQL that reads back what a
 * Transition wrote.
 *
 * <p>{@link SeededResourceSubject} holds the same facts for the Resource graph. The two stay apart
 * because nothing here touches a link row and nothing there touches a post.
 *
 * <p>{@code negotiation-2} is seeded {@code SUBMITTED} with no Resources, so firing at it cannot
 * Spawn Resource Lifecycles or send the notifications that follow. Its post flags are public
 * enabled and private disabled, and it has no posts, so any post seen after a send came from that
 * send. {@code negotiation-6} is used only where the subject has to start in {@code DRAFT}.
 *
 * <p>Tests place a Negotiation in a State by writing the State name onto the row. Several
 * Transitions start from States no seeded row is in, and firing a path to reach one would apply
 * that path's post effects first.
 *
 * <p>Everything reads back through SQL rather than a repository or a DTO, so the tests pin the row
 * a Transition wrote.
 */
final class SeededNegotiationSubject {

  /** Seeded {@code SUBMITTED}, created by {@link #CREATOR}, no Resources and no posts. */
  static final String NO_RESOURCES = "negotiation-2";

  /** The only Negotiation the seed places in {@code DRAFT}. */
  static final String SEEDED_IN_DRAFT = "negotiation-6";

  /**
   * Seeded {@code SUBMITTED} with two Resources and no State for either. It is the only seeded
   * Negotiation with Resources still waiting for Spawn, so the Spawn tests use it.
   *
   * <p>Different people represent its two Resources ({@link #REPRESENTATIVE_OF_FIRST_RESOURCE} and
   * {@link #REPRESENTATIVE_OF_SECOND_RESOURCE}) and {@link #CREATOR} created it, so each
   * notification has one possible cause. Tests attach {@link #UNREPRESENTED_RESOURCE_ROW_ID} by hand
   * where a Resource with no representative matters.
   */
  static final String WITH_UNINITIALISED_RESOURCES = "negotiation-5";

  /** {@code biobank:1:collection:2}, one of {@link #WITH_UNINITIALISED_RESOURCES}'s Resources. */
  static final String FIRST_RESOURCE = "biobank:1:collection:2";

  /** The database row id of {@link #FIRST_RESOURCE}, which only the link table needs. */
  static final long FIRST_RESOURCE_ROW_ID = 5L;

  /** {@code biobank:3:collection:1}, the other seeded Resource of that Negotiation. */
  static final String SECOND_RESOURCE = "biobank:3:collection:1";

  static final long REPRESENTATIVE_OF_FIRST_RESOURCE = 109L;
  static final long REPRESENTATIVE_OF_SECOND_RESOURCE = 105L;

  /**
   * The only seeded Resource with no representative, linked to no Negotiation by the seed. {@link
   * #linkResource} attaches it where the test needs a Resource nobody can be told about.
   */
  static final long UNREPRESENTED_RESOURCE_ROW_ID = 10L;

  /** The {@code source_id} of {@link #UNREPRESENTED_RESOURCE_ROW_ID}. */
  static final String UNREPRESENTED_RESOURCE = "biobank:3:collection:4";

  /** The one caller offered every Transition, used by every test that fires. */
  static final long ADMIN = 101L;

  /** Person 108, who created both Negotiations above and holds no admin role. */
  static final long CREATOR = 108L;

  /** One row of the {@code post} table, in the terms a caller can observe it in. */
  record PostRow(String text, String type, Long createdBy, LocalDateTime creationDate) {}

  /** Writes a starting State straight onto the Negotiation row, naming it as a string. */
  static void putInState(JdbcTemplate jdbcTemplate, String negotiationId, String state) {
    jdbcTemplate.update(
        "update negotiation set current_state = ? where id = ?", state, negotiationId);
  }

  /** Puts both post flags in a known place, so that a Transition's effect on them is visible. */
  static void setPostFlags(JdbcTemplate jdbcTemplate, String negotiationId, PostFlags flags) {
    jdbcTemplate.update(
        "update negotiation set public_posts_enabled = ?, private_posts_enabled = ? where id = ?",
        flags.publicPostsEnabled(),
        flags.privatePostsEnabled(),
        negotiationId);
  }

  static PostFlags postFlags(JdbcTemplate jdbcTemplate, String negotiationId) {
    return jdbcTemplate.queryForObject(
        "select public_posts_enabled, private_posts_enabled from negotiation where id = ?",
        (row, index) -> new PostFlags(row.getBoolean(1), row.getBoolean(2)),
        negotiationId);
  }

  /** Every post of a Negotiation, oldest first. */
  static List<PostRow> posts(JdbcTemplate jdbcTemplate, String negotiationId) {
    return jdbcTemplate.query(
        "select text, type, created_by, creation_date from post"
            + " where negotiation_id = ? order by creation_date",
        (row, index) ->
            new PostRow(
                row.getString("text"),
                row.getString("type"),
                (Long) row.getObject("created_by"),
                row.getObject("creation_date", LocalDateTime.class)),
        negotiationId);
  }

  static int postCount(JdbcTemplate jdbcTemplate, String negotiationId) {
    return jdbcTemplate.queryForObject(
        "select count(*) from post where negotiation_id = ?", Integer.class, negotiationId);
  }

  /** Attaches a Resource to a Negotiation in a nominated State, {@code null} meaning "no State". */
  static void linkResource(
      JdbcTemplate jdbcTemplate, String negotiationId, long resourceRowId, String state) {
    jdbcTemplate.update(
        "insert into negotiation_resource_link (negotiation_id, resource_id, current_state)"
            + " values (?, ?, ?)",
        negotiationId,
        resourceRowId,
        state);
  }

  /**
   * Clears the State of every Resource of a Negotiation. Spawn acts only on Resources with no State,
   * so a test that fires more than one Transition restores this between arms.
   */
  static void clearResourceStates(JdbcTemplate jdbcTemplate, String negotiationId) {
    jdbcTemplate.update(
        "update negotiation_resource_link set current_state = null where negotiation_id = ?",
        negotiationId);
  }

  /** Writes a State onto one link row of a Negotiation, naming it as a string. */
  static void putResourceInState(
      JdbcTemplate jdbcTemplate, String negotiationId, long resourceRowId, String state) {
    jdbcTemplate.update(
        "update negotiation_resource_link set current_state = ?"
            + " where negotiation_id = ? and resource_id = ?",
        state,
        negotiationId,
        resourceRowId);
  }

  /** The State of every Resource of a Negotiation, keyed by the Resource's source id. */
  static Map<String, String> resourceStates(JdbcTemplate jdbcTemplate, String negotiationId) {
    Map<String, String> states = new HashMap<>();
    jdbcTemplate
        .query(
            "select resource.source_id, link.current_state from negotiation_resource_link link"
                + " join resource on resource.id = link.resource_id where link.negotiation_id = ?",
            (row, index) -> Map.entry(row.getString(1), String.valueOf(row.getString(2))),
            negotiationId)
        .forEach(
            entry ->
                states.put(
                    entry.getKey(), "null".equals(entry.getValue()) ? null : entry.getValue()));
    return states;
  }

  /** Backdates a Negotiation so the pending reminder's lookup finds it. */
  static void setCreationDateDaysAgo(JdbcTemplate jdbcTemplate, String negotiationId, int days) {
    jdbcTemplate.update(
        "update negotiation set creation_date = now() - make_interval(days => ?) where id = ?",
        days,
        negotiationId);
  }

  static LocalDateTime creationDate(JdbcTemplate jdbcTemplate, String negotiationId) {
    return jdbcTemplate.queryForObject(
        "select creation_date from negotiation where id = ?", LocalDateTime.class, negotiationId);
  }

  private SeededNegotiationSubject() {}
}
