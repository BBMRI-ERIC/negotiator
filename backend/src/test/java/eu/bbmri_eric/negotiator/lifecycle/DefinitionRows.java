package eu.bbmri_eric.negotiator.lifecycle;

import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Writes Definition Version rows straight to the six tables, for integration tests outside the
 * definition package that need a definition to exist.
 *
 * <p>It uses SQL because the entities, repositories, compiler and cache are package private, so a
 * test outside the package can reach only the tables. Assertions on these rows rest on the schema
 * rather than on the entity mapping.
 *
 * <p>The caller picks the cleanup. A test that shares its application context calls {@link
 * #deleteEverythingWritten()}, because resolution reads the whole table and a version left active
 * would leak into the next test. A test that dirties the context after every method needs no
 * cleanup, because Flyway cleans and migrates on every context build.
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
      long versionId, long fromStateId, long eventId, long toStateId, String... requiredAuthority) {
    return jdbc.queryForObject(
        """
        INSERT INTO transition (lifecycle_definition_id, from_state_id, event_id, to_state_id,
                                required_authority)
        VALUES (?, ?, ?, ?, string_to_array(?, ',')) RETURNING id
        """,
        Long.class,
        versionId,
        fromStateId,
        eventId,
        toStateId,
        String.join(",", requiredAuthority));
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
   * Removes exactly the versions written through this instance. An unscoped {@code DELETE FROM
   * lifecycle_definition} would also remove seeded versions for every later test.
   *
   * <p>Children go first, and {@code action_wiring} goes through its Transition because that table
   * has no definition column.
   *
   * <p>The Compiled Graph cache keeps graphs for the deleted versions. That is harmless because the
   * sequence never reissues an id.
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
