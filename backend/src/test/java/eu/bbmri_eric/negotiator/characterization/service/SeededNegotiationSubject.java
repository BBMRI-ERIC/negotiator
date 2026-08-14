package eu.bbmri_eric.negotiator.characterization.service;

import eu.bbmri_eric.negotiator.characterization.service.NegotiationGraphV1.PostFlags;
import java.time.LocalDateTime;
import java.util.List;
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

  static LocalDateTime creationDate(JdbcTemplate jdbcTemplate, String negotiationId) {
    return jdbcTemplate.queryForObject(
        "select creation_date from negotiation where id = ?", LocalDateTime.class, negotiationId);
  }

  private SeededNegotiationSubject() {}
}
