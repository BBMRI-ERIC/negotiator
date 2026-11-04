package eu.bbmri_eric.negotiator.characterization.service;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Reads the notification rows the lifecycle-keyed handlers write, and maps each title to its
 * handler.
 *
 * <p>The tests read rows rather than mail because the row is a handler's only durable effect; a
 * separate asynchronous listener sends the mail, against a mail server no test has. The handlers
 * also run asynchronously, so read these rows under a bounded wait from {@link
 * LifecyclePersistence}, never straight after a send.
 *
 * <p>The five lifecycle-keyed strategies write six titles that no other lifecycle path writes. Two
 * are shared with other handlers: {@code "New Request"} is also written when a Negotiation is
 * created, and {@code "New Negotiation Request"} when Resources are added to a running Negotiation.
 * No test here does either, so within this package a title identifies its handler. A suite that
 * creates Negotiations cannot rely on that.
 *
 * <p>The titles are copied rather than imported because four of the five handlers are package
 * private. They reach users, so they are part of the observable behaviour.
 */
final class HandlerNotifications {

  /**
   * The five notification strategies that key on lifecycle identity, by the title they write.
   *
   * <p>{@link #NEGOTIATION_STATUS_CHANGE} has two titles: one for a Negotiation just submitted, one
   * for any later status change.
   */
  enum LifecycleHandler {
    /** Initialises a Negotiation's Resources and notifies their representatives. */
    NEGOTIATION_IN_PROGRESS("New Negotiation Request"),
    /** Tells the administrators that a Negotiation has been submitted for review. */
    NEGOTIATION_SUBMISSION("New Request"),
    /** Tells the Negotiation's creator that its status has moved. */
    NEGOTIATION_STATUS_CHANGE("Negotiation Submission Confirmed", "Request Status Update"),
    /** Tells the Negotiation's creator that one of its Resources has moved. */
    RESOURCE_STATE_CHANGE("Request Status update"),
    /** Reminds the representatives of Resources awaiting them. */
    PENDING_NEGOTIATION_REMINDER("Pending Negotiation Reminder");

    private final Set<String> titles;

    LifecycleHandler(String... titles) {
      this.titles = Set.of(titles);
    }

    Set<String> titles() {
      return titles;
    }

    boolean wrote(NotificationRow row) {
      return titles.contains(row.title());
    }
  }

  /** One row of the {@code notification} table, in the terms a recipient can observe it in. */
  record NotificationRow(long recipientId, String negotiationId, String title, String message) {}

  private static final String COLUMNS = "recipient_id, negotiation_id, title, message";

  /** Every notification written so far, oldest first. */
  static List<NotificationRow> all(JdbcTemplate jdbcTemplate) {
    return jdbcTemplate.query("select " + COLUMNS + " from notification order by id", rowReader());
  }

  /** Every notification attached to one Negotiation, oldest first. */
  static List<NotificationRow> forNegotiation(JdbcTemplate jdbcTemplate, String negotiationId) {
    return jdbcTemplate.query(
        "select " + COLUMNS + " from notification where negotiation_id = ? order by id",
        rowReader(),
        negotiationId);
  }

  /** Every notification one handler wrote, anywhere, oldest first. */
  static List<NotificationRow> writtenBy(JdbcTemplate jdbcTemplate, LifecycleHandler handler) {
    return all(jdbcTemplate).stream().filter(handler::wrote).toList();
  }

  /** The recipients one handler notified about one Negotiation. */
  static Set<Long> recipientsOf(
      JdbcTemplate jdbcTemplate, LifecycleHandler handler, String negotiationId) {
    return forNegotiation(jdbcTemplate, negotiationId).stream()
        .filter(handler::wrote)
        .map(NotificationRow::recipientId)
        .collect(Collectors.toUnmodifiableSet());
  }

  /**
   * Which of the five handlers have written anything at all so far.
   *
   * <p>Returns the whole set because "only this handler fired" is a claim about all five.
   */
  static Set<LifecycleHandler> handlersThatFired(JdbcTemplate jdbcTemplate) {
    List<NotificationRow> rows = all(jdbcTemplate);
    return Arrays.stream(LifecycleHandler.values())
        .filter(handler -> rows.stream().anyMatch(handler::wrote))
        .collect(Collectors.toUnmodifiableSet());
  }

  /**
   * Empties the table, so that what is read afterwards is what one send produced.
   *
   * <p>The seed has no notifications, but a test that fires more than one Transition needs to
   * separate them.
   */
  static void clear(JdbcTemplate jdbcTemplate) {
    jdbcTemplate.update("delete from notification");
  }

  private static org.springframework.jdbc.core.RowMapper<NotificationRow> rowReader() {
    return (row, index) ->
        new NotificationRow(
            row.getLong("recipient_id"),
            row.getString("negotiation_id"),
            row.getString("title"),
            row.getString("message"));
  }

  private HandlerNotifications() {}
}
