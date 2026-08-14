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
 * <p>The counterpart of {@link SeededResourceSubject}, which holds the same facts for the Resource
 * graph's subject. The two are kept apart because they are different subjects with different
 * hazards, not two copies of one helper: nothing here touches a link row, and nothing there touches
 * a post.
 *
 * <p><b>Why {@code negotiation-2}.</b> It is seeded {@code SUBMITTED} and has no Resources at all,
 * so driving it can neither Spawn Resource Lifecycles nor set off the notifications that follow
 * them - the same reason {@link NegotiationTransitionParityTest} carries its traffic on it. Its
 * seeded post flags are public enabled, private disabled, and it carries no posts, so a post
 * observed after a send is one the send created. {@code negotiation-6} is used only where the
 * subject has to start in {@code DRAFT} without being put there by hand.
 *
 * <p><b>Placing a Negotiation in a State.</b> Written straight onto the row, naming the State as a
 * string, for the reason {@link SeededResourceSubject} gives: several Transitions start from States
 * no seeded row is in, and driving a path to reach one would apply that path's own post effects
 * first, which is exactly what a row about one Transition must not have happened to it.
 *
 * <p><b>Everything is read back through SQL</b> rather than through a repository or a DTO, so that
 * what is pinned is the row a Transition wrote and not a mapping layer's view of it.
 */
final class SeededNegotiationSubject {

  /** Seeded {@code SUBMITTED}, created by {@link #CREATOR}, no Resources and no posts. */
  static final String NO_RESOURCES = "negotiation-2";

  /** The only Negotiation the seed places in {@code DRAFT}. */
  static final String SEEDED_IN_DRAFT = "negotiation-6";

  /**
   * Seeded {@code SUBMITTED} with two Resources and no State recorded for either, which is what
   * makes it the subject of the spawn tests: it is the only seeded Negotiation that still has
   * Resources waiting to be initialised.
   *
   * <p>Its two seeded Resources are represented by two different people - {@link
   * #REPRESENTATIVE_OF_FIRST_RESOURCE} and {@link #REPRESENTATIVE_OF_SECOND_RESOURCE} - and it is
   * created by {@link #CREATOR}, so who was notified is a question with a discriminating answer.
   * {@link #UNREPRESENTED_RESOURCE_ROW_ID} is attached by hand where the third case matters.
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
   * The only seeded Resource with no representative at all, linked to no Negotiation by the seed.
   * Attached by {@link #linkResource} where "a Resource nobody can be told about" is the case under
   * test.
   */
  static final long UNREPRESENTED_RESOURCE_ROW_ID = 10L;

  /** The {@code source_id} of {@link #UNREPRESENTED_RESOURCE_ROW_ID}. */
  static final String UNREPRESENTED_RESOURCE = "biobank:3:collection:4";

  /** The caller every driving test uses: the one caller offered every Transition. */
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
   * Puts every Resource of a Negotiation back to having no recorded State, which is the only
   * condition under which spawn does anything at all - so a walk that fires more than one
   * Transition has to restore it between arms or every arm after the first is vacuous.
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

  /** Backdates a Negotiation, which is how the pending reminder's own lookup is satisfied. */
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
