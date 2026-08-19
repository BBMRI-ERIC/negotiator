-- Guard wiring and Action wiring — the configuration attaching a Guard or an Action to where it
-- applies. Guards and Actions never interleave: Guards run before a commit, Actions only after one,
-- so they have separate tables with independent ordering. Only Guards need a scope (definition-wide
-- vs one Transition); Actions are always transition-scoped.

-- transition gains a UNIQUE (lifecycle_definition_id, id) so a composite FK can reference the pair,
-- the same pattern slice 03 applied to state and event. Redundant as a uniqueness claim — id alone
-- is the primary key — but its whole job is to be a referenceable pair.
ALTER TABLE transition
    ADD CONSTRAINT uq_transition_id_with_definition UNIQUE (lifecycle_definition_id, id);

-- Guard wiring. A nullable transition_id is the scope: null means the Guard applies to every
-- Transition of the Definition Version; set means that Transition alone. Two partial unique indexes
-- keep sort_order unique within each of those two scopes independently.
CREATE TABLE guard_wiring
(
    id                      BIGSERIAL PRIMARY KEY,
    lifecycle_definition_id BIGINT       NOT NULL,
    transition_id           BIGINT,
    type_key                VARCHAR(255) NOT NULL,
    params                  JSONB,
    sort_order              INTEGER      NOT NULL,
    CONSTRAINT fk_guard_wiring_lifecycle_definition FOREIGN KEY (lifecycle_definition_id)
        REFERENCES lifecycle_definition (id) ON DELETE RESTRICT,
    CONSTRAINT fk_guard_wiring_transition FOREIGN KEY (lifecycle_definition_id, transition_id)
        REFERENCES transition (lifecycle_definition_id, id) ON DELETE RESTRICT
);

-- sort_order is unique within each of the Guard's two scopes independently. A definition-scoped
-- Guard (transition_id null) shares one sequence with every other definition-scoped Guard; a
-- transition-scoped Guard shares another with only the Guards on that Transition. The two partial
-- indexes do not overlap, so the same sort_order is accepted across scopes.
CREATE UNIQUE INDEX uq_guard_wiring_sort_order_definition
    ON guard_wiring (lifecycle_definition_id, sort_order)
    WHERE transition_id IS NULL;

CREATE UNIQUE INDEX uq_guard_wiring_sort_order_transition
    ON guard_wiring (transition_id, sort_order)
    WHERE transition_id IS NOT NULL;

-- Action wiring. Always transition-scoped: no definition reference at all, since the Transition
-- already implies it. Guards run before a commit and Actions only after one, so the two tables have
-- independent ordering — a shared sort_order column across them would be meaningless.
CREATE TABLE action_wiring
(
    id            BIGSERIAL PRIMARY KEY,
    transition_id BIGINT       NOT NULL,
    type_key      VARCHAR(255) NOT NULL,
    params        JSONB,
    sort_order    INTEGER      NOT NULL,
    CONSTRAINT fk_action_wiring_transition FOREIGN KEY (transition_id)
        REFERENCES transition (id) ON DELETE RESTRICT
);

-- sort_order is unique within a Transition's Action chain, for the same reason as Guard wiring:
-- a duplicate would make the chain non-deterministic. Not a partial index — transition_id is
-- always NOT NULL — so a plain UNIQUE constraint.
ALTER TABLE action_wiring
    ADD CONSTRAINT uq_action_wiring_transition_sort_order UNIQUE (transition_id, sort_order);
