package eu.bbmri_eric.negotiator.lifecycle;

import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Writes Definition Version rows straight to the six tables, for the integration tests that need a
 * definition to exist and cannot build one any other way.
 *
 * <p><b>Why SQL and not the entities.</b> Every type below {@code LifecycleDefinitions} is package
 * private — the six entities, their repositories, the compiler, the cache — and that is the
 * definition package's whole shape rather than an oversight, so a test outside it has the tables
 * and nothing else. That is also how ADR 0009's v1 seed arrives, which means assertions resting on
 * these rows rest on the schema rather than on a mapping that agrees with itself.
 *
 * <p>Extracted when the second integration test needed the same six writers: the Event Firing slab
 * would otherwise have carried a near-verbatim copy of {@code
 * CompiledGraphResolutionIntegrationTest}'s, and a third slab a third.
 *
 * <p><b>Cleanup is the caller's choice, because the two callers genuinely differ.</b> {@link
 * #deleteEverythingWritten()} is for a test that shares its application context and must take back
 * exactly what it added — resolution questions are asked of the whole table, so a version left
 * active would be answered to the next test rather than to nobody. A test that dirties the context
 * after every method needs none of it: the Flyway strategy is clean-and-migrate on every context
 * build. Neither is the default, so a caller has to decide which it is.
 */
final class DefinitionRows {

  private final JdbcTemplate jdbc;

  /** Every version written through this instance, in insertion order. */
  private final List<Long> versionsWritten = new ArrayList<>();

  DefinitionRows(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  long writeVersion(
      String familyKey, String scope, int version, boolean active, boolean globalDefault) {
    long versionId =
        jdbc.queryForObject(
            """
            INSERT INTO lifecycle_definition (scope, family_key, name, version, active,
                                              is_global_default)
            VALUES (?, ?, ?, ?, ?, ?) RETURNING id
            """,
            Long.class,
            scope,
            familyKey,
            familyKey,
            version,
            active,
            globalDefault);
    versionsWritten.add(versionId);
    return versionId;
  }

  long writeState(long versionId, String name, boolean initial, boolean terminal) {
    return jdbc.queryForObject(
        """
        INSERT INTO state (lifecycle_definition_id, name, label, initial, terminal)
        VALUES (?, ?, ?, ?, ?) RETURNING id
        """,
        Long.class,
        versionId,
        name,
        name,
        initial,
        terminal);
  }

  long writeEvent(long versionId, String name) {
    return jdbc.queryForObject(
        "INSERT INTO event (lifecycle_definition_id, name) VALUES (?, ?) RETURNING id",
        Long.class,
        versionId,
        name);
  }

  long writeTransition(
      long versionId, long fromStateId, long eventId, long toStateId, String requiredAuthority) {
    return jdbc.queryForObject(
        """
        INSERT INTO transition (lifecycle_definition_id, from_state_id, event_id, to_state_id,
                                required_authority)
        VALUES (?, ?, ?, ?, ?) RETURNING id
        """,
        Long.class,
        versionId,
        fromStateId,
        eventId,
        toStateId,
        requiredAuthority);
  }

  /**
   * @param transitionId null for a Guard that applies to every Transition of the version
   */
  void writeGuardWiring(long versionId, Long transitionId, String typeKey, int sortOrder) {
    jdbc.update(
        """
        INSERT INTO guard_wiring (lifecycle_definition_id, transition_id, type_key, sort_order)
        VALUES (?, CAST(? AS bigint), ?, ?)
        """,
        versionId,
        transitionId,
        typeKey,
        sortOrder);
  }

  void writeActionWiring(long transitionId, String typeKey, String params, int sortOrder) {
    jdbc.update(
        """
        INSERT INTO action_wiring (transition_id, type_key, params, sort_order)
        VALUES (?, ?, CAST(? AS jsonb), ?)
        """,
        transitionId,
        typeKey,
        params,
        sortOrder);
  }

  /**
   * Takes back exactly the versions written through this instance, and nothing else.
   *
   * <p>Scoped rather than {@code DELETE FROM lifecycle_definition}, which would be correct today
   * and stay correct right up until the migration slab lands ADR 0009's v1 seed — at which point it
   * would wipe it for every test that ran afterwards, and the failure would surface anywhere but
   * here.
   *
   * <p>Children first, and {@code action_wiring} through its Transition, because that table carries
   * no definition column of its own.
   *
   * <p>What is deliberately <em>not</em> cleaned is the Compiled Graph cache, a singleton of the
   * shared application context that goes on holding graphs for versions these deletes have removed.
   * Harmless only because the sequence never reissues an id, so no later test can ask for one of
   * these versions and be answered from the cache.
   */
  void deleteEverythingWritten() {
    for (long versionId : versionsWritten) {
      jdbc.update(
          """
          DELETE FROM action_wiring
           WHERE transition_id IN (SELECT id FROM transition WHERE lifecycle_definition_id = ?)
          """,
          versionId);
      jdbc.update("DELETE FROM guard_wiring WHERE lifecycle_definition_id = ?", versionId);
      jdbc.update("DELETE FROM transition WHERE lifecycle_definition_id = ?", versionId);
      jdbc.update("DELETE FROM state WHERE lifecycle_definition_id = ?", versionId);
      jdbc.update("DELETE FROM event WHERE lifecycle_definition_id = ?", versionId);
      jdbc.update("DELETE FROM lifecycle_definition WHERE id = ?", versionId);
    }
    versionsWritten.clear();
  }
}
