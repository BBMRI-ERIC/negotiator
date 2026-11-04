package eu.bbmri_eric.negotiator.characterization.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Reads the two Lifecycle Record tables.
 *
 * <p>The tests speak of a recorded State as a string, and only this file knows it comes from a
 * {@code changed_to} column. If that column becomes a foreign key to a State row, the SQL here gains
 * a join and no assertion changes, just as {@link
 * eu.bbmri_eric.negotiator.characterization.adapter.LifecycleTestAdapter} does for the services.
 *
 * <p>It reads through SQL rather than a repository so the tests pin the row a Transition wrote.
 * Neither Record entity has a field for its Negotiation; the {@code negotiation_id} column comes from
 * the owning side's {@code @JoinColumn}, so a JPA read could not state the association.
 *
 * <p>A Resource appears as its {@code source_id}, which the Lifecycle keys on, not the Record's
 * {@code resource_id} row id. The join is a left join so a Record whose Resource is missing shows up
 * with no Resource instead of vanishing.
 *
 * <p>Rows come back in id order. The id is an identity column, so that is insertion order, and it is
 * the only order the trail has: a Record names only the State it changed to, so which Transition ran
 * depends on the row order.
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

    /** This row without the fields that always differ between two rows. */
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

    /** This row without the fields that always differ between two rows. */
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
