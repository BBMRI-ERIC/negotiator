package eu.bbmri_eric.negotiator.lifecycle.firing;

/**
 * A Lifecycle was asked to move before it had started: it carries no Definition Version Pin, or no
 * State, or neither.
 *
 * <p><b>Not a refusal.</b> ADR 0005's four failure categories describe a move the domain disallows,
 * and each maps to a status a caller can act on — 403, 422, 409. This is none of those: there is no
 * graph to judge the move against and no State to judge it from, so the pipeline never runs and
 * there is no category to report. Refusing it as {@code NO_TRANSITION} would tell a caller that
 * their Event is wrong when the row is.
 *
 * <p><b>Both causes are one exception because both are the same fact.</b> A Resource Lifecycle
 * begins at Spawn, which resolves the Definition Family, pins the version and writes the initial
 * State together. A row with one and not the other is a Spawn that half happened, and a caller can
 * do nothing different about either half; the message says which is missing so that whoever reads
 * the log can.
 *
 * <p>Expected rather than remote, today: both pin columns are nullable and 100% NULL until the
 * migration slab backfills them and sets them {@code NOT NULL}, so every row in every environment
 * is unpinned right now. A test that wants a pinned Lifecycle writes the pin itself.
 */
public class UnstartedLifecycleException extends RuntimeException {

  UnstartedLifecycleException(String message) {
    super(message);
  }
}
