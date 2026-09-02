package eu.bbmri_eric.negotiator.lifecycle.evaluation;

import eu.bbmri_eric.negotiator.lifecycle.graph.ActionContext;
import org.springframework.stereotype.Component;

/**
 * Starts the Resource Lifecycles of a Negotiation. <b>Registered here, and nothing more</b> — the
 * type key exists so a Wiring row can name it and a definition mentioning it can compile. The body
 * writes, and issue 09 excludes everything that writes.
 *
 * <p>Registration alone is worth doing because the alternative is that the v1 seed cannot be
 * written until the coupling slab lands: a row naming a key no strategy declares fails a compile,
 * which is the behaviour this prototype deliberately chose.
 *
 * <p><b>Three things the slab that fills this in must not inherit from the documents.</b> ADR 0007
 * and ADR 0009 both specify this Action against a picture of spawn that the code does not have, and
 * the characterization suite pinned the real one:
 *
 * <ul>
 *   <li>It must <em>not</em> set the State to the definition's initial one. Spawn writes {@code
 *       REPRESENTATIVE_CONTACTED} for a Resource that has a representative and {@code
 *       REPRESENTATIVE_UNREACHABLE} for one that does not, and never the graph's initial {@code
 *       SUBMITTED}. Starting at the initial State would move every representative one step
 *       backwards and re-offer them admin-only Events, so they would be offered nothing at all
 *       where today they are contacted.
 *   <li>Wiring it to the approval Transition alone loses the resume case. Spawn keys on
 *       <em>arriving</em> at {@code IN_PROGRESS}, so unpausing spawns today, and a single row on
 *       approval is a behaviour change for every resumed Negotiation.
 *   <li>It must publish {@code ResourceLifecyclesSpawnedEvent} and must <em>not</em> publish {@code
 *       ResourceStateChangeEvent}. Spawn announces nothing today, so emitting a per-Resource state
 *       change would add N notifications and N webhook deliveries per approval.
 * </ul>
 *
 * <p>No params: what to spawn follows from the Negotiation's own Resources, and ADR 0007 is
 * explicit that Resource definitions are the <em>targets</em> of spawn and carry no spawn
 * configuration at all, so adding a new Resource family wires nothing.
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
        ("Spawning Resource Lifecycles is not built yet — this strategy is registered so that a "
                + "Wiring row naming '%s' can compile. Asked to spawn for Negotiation %s on %s. "
                + "Read this class's javadoc before implementing it: ADR 0007 and ADR 0009 both "
                + "specify it against a picture of spawn that the code does not have.")
            .formatted(
                TYPE_KEY,
                context.evaluation().subject().negotiationId(),
                context.committed().event()));
  }
}
