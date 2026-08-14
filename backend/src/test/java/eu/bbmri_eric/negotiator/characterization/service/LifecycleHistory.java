package eu.bbmri_eric.negotiator.characterization.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The suite's single reader of the two Lifecycle Record tables - the audit trail a Transition
 * leaves behind.
 *
 * <p><b>Why one reader, and why it matters more here than anywhere else.</b> ADR 0008 converts the
 * {@code changed_to} column of both tables from a State name into a {@code state_id} foreign key to
 * a State row. Every assertion about what a Transition recorded therefore has to survive a change
 * in how the State is stored, and the only way to write such an assertion is to keep the storage
 * detail out of it: the tests below speak of a recorded State as a string, and this file is the one
 * place that knows the string comes out of a column called {@code changed_to}. At cutover this
 * file's SQL grows a join and every assertion written against it stays byte-identical - the same
 * argument, and the same shape, as {@link
 * eu.bbmri_eric.negotiator.characterization.adapter.LifecycleTestAdapter} makes for the services.
 *
 * <p><b>Read through SQL, not through a repository.</b> For the reason {@link
 * SeededNegotiationSubject} gives: what is pinned is the row a Transition wrote, not a mapping
 * layer's view of it. In particular neither Record entity carries a field for the Negotiation it
 * belongs to - the association is a {@code negotiation_id} column written by the owning side's
 * {@code @JoinColumn} - so a JPA read could not state the association at all without going back
 * through the Negotiation that was just asserted about.
 *
 * <p><b>A Resource is identified by its {@code source_id}.</b> The Record's own {@code resource_id}
 * is the Resource's row id, but every other statement the suite makes about a Resource is in terms
 * of the {@code source_id} the Lifecycle keys on, so the join happens here rather than leaking a
 * row id into an assertion. The join is a left join deliberately: a Record whose Resource is
 * missing must show up as a Record with no Resource rather than disappear from the trail.
 *
 * <p><b>Ordered by id.</b> The id is an identity column, so id order is insertion order, and that
 * is the only order the trail has: a Record names the State it changed <em>to</em> and nothing
 * else, so reconstructing which Transition ran depends entirely on the order of the rows. That is a
 * fact the migration inherits, and {@code NegotiationHistoryRowsTest} pins it.
 */
final class LifecycleHistory {

  /** One row of {@code negotiation_lifecycle_record}, in the terms a caller can observe it in. */
  record NegotiationRecord(
      long id,
      String negotiationId,
      String changedTo,
      Long createdBy,
      Long modifiedBy,
      LocalDateTime creationDate,
      LocalDateTime modifiedDate) {

    /** This row with everything that necessarily differs between two rows removed. */
    NegotiationRecord withoutIdentityAndTimestamps() {
      return new NegotiationRecord(0L, negotiationId, changedTo, createdBy, modifiedBy, null, null);
    }
  }

  /**
   * One row of {@code negotiation_resource_lifecycle_record}. {@code resourceSourceId} is the
   * Resource's {@code source_id}, resolved from the row's {@code resource_id}.
   */
  record ResourceRecord(
      long id,
      String negotiationId,
      String resourceSourceId,
      String changedTo,
      Long createdBy,
      Long modifiedBy,
      LocalDateTime creationDate,
      LocalDateTime modifiedDate) {

    /** This row with everything that necessarily differs between two rows removed. */
    ResourceRecord withoutIdentityAndTimestamps() {
      return new ResourceRecord(
          0L, negotiationId, resourceSourceId, changedTo, createdBy, modifiedBy, null, null);
    }
  }

  private static final String NEGOTIATION_RECORD_COLUMNS =
      "id, negotiation_id, changed_to, created_by, modified_by, creation_date, modified_date";

  private static final String RESOURCE_RECORD_COLUMNS =
      "history.id, history.negotiation_id, resource.source_id, history.changed_to,"
          + " history.created_by, history.modified_by, history.creation_date,"
          + " history.modified_date";

  /** Every Negotiation Lifecycle Record in the database, oldest first. */
  static List<NegotiationRecord> allNegotiationRecords(JdbcTemplate jdbcTemplate) {
    return jdbcTemplate.query(
        "select " + NEGOTIATION_RECORD_COLUMNS + " from negotiation_lifecycle_record order by id",
        LifecycleHistory::readNegotiationRecord);
  }

  /** The Negotiation Lifecycle Records of one Negotiation, oldest first. */
  static List<NegotiationRecord> negotiationRecordsOf(
      JdbcTemplate jdbcTemplate, String negotiationId) {
    return jdbcTemplate.query(
        "select "
            + NEGOTIATION_RECORD_COLUMNS
            + " from negotiation_lifecycle_record where negotiation_id = ? order by id",
        LifecycleHistory::readNegotiationRecord,
        negotiationId);
  }

  /** The States one Negotiation's trail records, in the order the rows were written. */
  static List<String> negotiationStatesRecordedFor(
      JdbcTemplate jdbcTemplate, String negotiationId) {
    return negotiationRecordsOf(jdbcTemplate, negotiationId).stream()
        .map(NegotiationRecord::changedTo)
        .toList();
  }

  /** Every Resource Lifecycle Record in the database, oldest first. */
  static List<ResourceRecord> allResourceRecords(JdbcTemplate jdbcTemplate) {
    return jdbcTemplate.query(
        "select "
            + RESOURCE_RECORD_COLUMNS
            + " from negotiation_resource_lifecycle_record history"
            + " left join resource on resource.id = history.resource_id"
            + " order by history.id",
        LifecycleHistory::readResourceRecord);
  }

  /** The Resource Lifecycle Records of one Negotiation, oldest first. */
  static List<ResourceRecord> resourceRecordsOf(JdbcTemplate jdbcTemplate, String negotiationId) {
    return jdbcTemplate.query(
        "select "
            + RESOURCE_RECORD_COLUMNS
            + " from negotiation_resource_lifecycle_record history"
            + " left join resource on resource.id = history.resource_id"
            + " where history.negotiation_id = ? order by history.id",
        LifecycleHistory::readResourceRecord,
        negotiationId);
  }

  /** The States one Negotiation's Resource trail records, in the order the rows were written. */
  static List<String> resourceStatesRecordedFor(JdbcTemplate jdbcTemplate, String negotiationId) {
    return resourceRecordsOf(jdbcTemplate, negotiationId).stream()
        .map(ResourceRecord::changedTo)
        .toList();
  }

  private static NegotiationRecord readNegotiationRecord(ResultSet row, int index)
      throws SQLException {
    return new NegotiationRecord(
        row.getLong("id"),
        row.getString("negotiation_id"),
        row.getString("changed_to"),
        (Long) row.getObject("created_by"),
        (Long) row.getObject("modified_by"),
        row.getObject("creation_date", LocalDateTime.class),
        row.getObject("modified_date", LocalDateTime.class));
  }

  private static ResourceRecord readResourceRecord(ResultSet row, int index) throws SQLException {
    return new ResourceRecord(
        row.getLong("id"),
        row.getString("negotiation_id"),
        row.getString("source_id"),
        row.getString("changed_to"),
        (Long) row.getObject("created_by"),
        (Long) row.getObject("modified_by"),
        row.getObject("creation_date", LocalDateTime.class),
        row.getObject("modified_date", LocalDateTime.class));
  }

  private LifecycleHistory() {}
}
