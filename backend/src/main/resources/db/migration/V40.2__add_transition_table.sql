-- A Transition must not connect States or Events of two Definition Versions, or loading a graph by
-- its Definition Version id would return edges that point out of it. A composite foreign key on
-- (lifecycle_definition_id, id) prevents that, and PostgreSQL accepts one only against a unique
-- constraint, so each vertex table gets one. id is already the primary key, so the constraint adds
-- no uniqueness. It exists only so the pair can be referenced.
ALTER TABLE state
    ADD CONSTRAINT uq_state_id_with_definition UNIQUE (lifecycle_definition_id, id);

ALTER TABLE event
    ADD CONSTRAINT uq_event_id_with_definition UNIQUE (lifecycle_definition_id, id);

-- required_authority is its own column and not a Guard. Asking who is firing and asking whether the
-- domain permits the move fail differently, so they must not be the same kind of row.
--
-- A caller holding any one value of the set may fire the Transition. Six of the eight Negotiation
-- Transitions allow an admin or the creator, which is {IS_ADMIN, IS_CREATOR}. The set is an array
-- and not a join table because code reads it whole with its Transition and never queries it by
-- element, and an array keeps the CHECK below on the one table it constrains.
CREATE TABLE transition
(
    id                      BIGSERIAL PRIMARY KEY,
    lifecycle_definition_id BIGINT       NOT NULL,
    from_state_id           BIGINT       NOT NULL,
    to_state_id             BIGINT       NOT NULL,
    event_id                BIGINT       NOT NULL,
    required_authority      VARCHAR(255)[] NOT NULL,
    CONSTRAINT fk_transition_lifecycle_definition FOREIGN KEY (lifecycle_definition_id)
        REFERENCES lifecycle_definition (id) ON DELETE RESTRICT,
    CONSTRAINT fk_transition_from_state FOREIGN KEY (lifecycle_definition_id, from_state_id)
        REFERENCES state (lifecycle_definition_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_transition_to_state FOREIGN KEY (lifecycle_definition_id, to_state_id)
        REFERENCES state (lifecycle_definition_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_transition_event FOREIGN KEY (lifecycle_definition_id, event_id)
        REFERENCES event (lifecycle_definition_id, id) ON DELETE RESTRICT
);

-- Every element is one of the four values, the array is one-dimensional with no NULL element, and
-- SYSTEM only appears alone. An empty array is allowed and admits any person. Only the system may
-- fire a System Event and no person is ever offered one, so SYSTEM beside a person value would
-- contradict itself. Definition Compilation checks the same rules again, because it reads rows and
-- does not trust this constraint.
ALTER TABLE transition
    ADD CONSTRAINT transition_required_authority_check CHECK (
        array_ndims(required_authority) = 1
            AND array_position(required_authority, NULL) IS NULL
            AND required_authority <@ ARRAY ['IS_ADMIN', 'IS_CREATOR', 'IS_REPRESENTATIVE', 'SYSTEM']::VARCHAR[]
            AND (cardinality(required_authority) <= 1
            OR NOT 'SYSTEM' = ANY (required_authority))
        );

-- The Transition Evaluator asks which Transition leaves State X for Event Y, and the answer must be
-- a single row, because it has no way to choose between duplicates.
ALTER TABLE transition
    ADD CONSTRAINT uq_transition_definition_source_event
        UNIQUE (lifecycle_definition_id, from_state_id, event_id);

-- That constraint's index leads with the definition, so it cannot serve the lookup above. This
-- index does.
CREATE INDEX idx_transition_source_event ON transition (from_state_id, event_id);
