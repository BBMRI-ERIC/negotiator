package eu.bbmri_eric.negotiator.characterization.service;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The suite's single reader of the notification rows the lifecycle-keyed handlers write, and the
 * one place that knows which title belongs to which handler.
 *
 * <p><b>Why the rows and not the mail.</b> A handler's only durable, synchronously inspectable
 * effect is the notification row it creates; sending the mail is a separate asynchronous listener
 * on top of that row, against a mail server no test has. Reading the rows is what makes the handler
 * assertions fast and free of SMTP, which is the parent PRD's story 20. The handlers themselves are
 * dispatched asynchronously, so every read of these rows belongs under a bounded wait from {@link
 * LifecyclePersistence}, never immediately after a send.
 *
 * <p><b>Why a title identifies a handler here, and where that stops being true.</b> Of the eight
 * notification strategies, the five that key on lifecycle identity write six distinct titles, none
 * of which any other lifecycle-triggered path writes. Two titles are nevertheless shared with
 * handlers that are <em>not</em> lifecycle-keyed: {@code "New Request"} is also written when a
 * Negotiation is created, and {@code "New Negotiation Request"} is also written when Resources are
 * added to a running Negotiation. Neither happens in these tests - nothing here creates a
 * Negotiation, and the only Resources ever named are already linked - so within this package a
 * title identifies its handler. A later suite that creates Negotiations must not inherit that
 * assumption.
 *
 * <p><b>Titles are transcribed, not imported.</b> Four of the five handlers are package private, so
 * their constants are unreachable from here. The strings below are the observable contract all the
 * same: they reach the user.
 */
final class HandlerNotifications {

  /**
   * The five notification strategies that key on lifecycle identity, by the title they write.
   *
   * <p>{@link #NEGOTIATION_STATUS_CHANGE} carries two, because it writes a different title for a
   * Negotiation that has just been submitted than for one whose status has moved on afterwards.
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
   * <p>Returning the whole set rather than answering one handler at a time is what makes a negative
   * claim worth making: "only this handler fired" is a statement about all five.
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
   * <p>The seed carries no notifications, but a test that drives more than one Transition needs a
   * line between them, and a count taken from zero says more than a difference between two counts.
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
