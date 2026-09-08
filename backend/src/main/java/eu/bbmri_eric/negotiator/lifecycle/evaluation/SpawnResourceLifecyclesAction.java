package eu.bbmri_eric.negotiator.lifecycle.evaluation;

import eu.bbmri_eric.negotiator.lifecycle.graph.ActionContext;
import org.springframework.stereotype.Component;

/**
 * Starts the Resource Lifecycles of a Negotiation. Only the type key exists so far, so a Wiring row
 * can name it and a Definition Version that uses it can compile. The body throws.
 *
 * <p>The implementation must match how Spawn behaves on the Spring Statemachine path:
 *
 * <ul>
 *   <li>It sets {@code REPRESENTATIVE_CONTACTED} for a Resource with a representative and {@code
 *       REPRESENTATIVE_UNREACHABLE} for one without, never the initial {@code SUBMITTED}. Starting
 *       at {@code SUBMITTED} would leave representatives with no Possible Events.
 *   <li>It runs on every arrival at {@code IN_PROGRESS}, including a resume, not only on
 *       approval.
 *   <li>It publishes {@code ResourceLifecyclesSpawnedEvent} and not {@code
 *       ResourceStateChangeEvent}, which would add one notification and one webhook delivery per
 *       Resource.
 * </ul>
 *
 * <p>It takes no params because the Negotiation's own Resources decide what to spawn, and
 * Resource-scope definitions carry no Spawn configuration.
 */
@Component
class SpawnResourceLifecyclesAction implements Action<NoParams> {

  static final String TYPE_KEY = "SPAWN_RESOURCE_LIFECYCLES";

  @Override
  public String typeKey() {
    return TYPE_KEY;
  }

  @Override
  public Class<NoParams> paramsType() {
    return NoParams.class;
  }

  @Override
  public void run(ActionContext context, NoParams params) {
    throw new UnsupportedOperationException(
        ("Spawning Resource Lifecycles is not built yet. This strategy is registered so that a "
                + "Wiring row naming '%s' can compile. Asked to spawn for Negotiation %s on %s. "
                + "Read this class's javadoc before implementing it.")
            .formatted(
                TYPE_KEY,
                context.evaluation().subject().negotiationId(),
                context.committed().event()));
  }
}
