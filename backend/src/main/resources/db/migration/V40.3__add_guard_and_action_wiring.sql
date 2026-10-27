-- Guards run before a commit and Actions only after one, so they never interleave and get separate
-- Wiring tables with independent ordering. Only Guard Wiring has a scope, either a whole Definition
-- Version or one Transition. Action Wiring always belongs to one Transition.

-- A composite foreign key can reference (lifecycle_definition_id, id) only through a unique
-- constraint on the pair, as on state and event. id alone is already the primary key.
ALTER TABLE transition
    ADD CONSTRAINT uq_transition_id_with_definition UNIQUE (lifecycle_definition_id, id);

-- transition_id is the scope. Null means the Guard applies to every Transition of the Definition
-- Version, and a value means that Transition alone.
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

-- Each scope has its own sort_order sequence. Definition-wide Guards share one, and the Guards on
-- one Transition share another. The two partial indexes do not overlap, so the same sort_order may
-- appear in both scopes.
CREATE UNIQUE INDEX uq_guard_wiring_sort_order_definition
    ON guard_wiring (lifecycle_definition_id, sort_order)
    WHERE transition_id IS NULL;

CREATE UNIQUE INDEX uq_guard_wiring_sort_order_transition
    ON guard_wiring (transition_id, sort_order)
    WHERE transition_id IS NOT NULL;

-- Action Wiring has no definition column, because its Transition already names the Definition
-- Version.
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

-- A duplicate sort_order would leave the order of a Transition's Actions undefined.
ALTER TABLE action_wiring
    ADD CONSTRAINT uq_action_wiring_transition_sort_order UNIQUE (transition_id, sort_order);
