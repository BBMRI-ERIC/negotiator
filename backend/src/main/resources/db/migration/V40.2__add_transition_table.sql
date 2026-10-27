-- A Transition must not connect States or Events of two Definition Versions, or loading a graph by
-- its Definition Version id would return edges that point out of it. A composite foreign key on
-- (lifecycle_definition_id, id) prevents that, and PostgreSQL accepts one only against a unique
-- constraint, so each vertex table gets one. id is already the primary key, so the constraint adds
-- no uniqueness. It exists only so the pair can be referenced.
ALTER TABLE state
    ADD CONSTRAINT uq_state_id_with_definition UNIQUE (lifecycle_definition_id, id);

ALTER TABLE event
    ADD CONSTRAINT uq_event_id_with_definition UNIQUE (lifecycle_definition_id, id);

-- required_authority is a column of its own and not a Guard, deliberately: asking who is firing and
-- asking whether the move is currently legal fail differently, so they must not be expressible as the
-- same kind of row. Single-valued: the six Transitions that behave like "admin or creator" have no
-- spelling here, and inventing one is a decision of its own.
CREATE TABLE transition
(
    id                      BIGSERIAL PRIMARY KEY,
    lifecycle_definition_id BIGINT       NOT NULL,
    from_state_id           BIGINT       NOT NULL,
    to_state_id             BIGINT       NOT NULL,
    event_id                BIGINT       NOT NULL,
    required_authority      VARCHAR(255) NOT NULL,
    CONSTRAINT fk_transition_lifecycle_definition FOREIGN KEY (lifecycle_definition_id)
        REFERENCES lifecycle_definition (id) ON DELETE RESTRICT,
    CONSTRAINT fk_transition_from_state FOREIGN KEY (lifecycle_definition_id, from_state_id)
        REFERENCES state (lifecycle_definition_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_transition_to_state FOREIGN KEY (lifecycle_definition_id, to_state_id)
        REFERENCES state (lifecycle_definition_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_transition_event FOREIGN KEY (lifecycle_definition_id, event_id)
        REFERENCES event (lifecycle_definition_id, id) ON DELETE RESTRICT
);

ALTER TABLE transition
    ADD CONSTRAINT transition_required_authority_check CHECK (((required_authority)::text = ANY ((ARRAY['NONE'::character varying, 'IS_ADMIN'::character varying, 'IS_CREATOR'::character varying, 'IS_REPRESENTATIVE'::character varying, 'SYSTEM'::character varying])::text[])));

-- The Transition Evaluator asks which Transition leaves State X for Event Y, and the answer must be
-- a single row, because it has no way to choose between duplicates.
ALTER TABLE transition
    ADD CONSTRAINT uq_transition_definition_source_event
        UNIQUE (lifecycle_definition_id, from_state_id, event_id);

-- That constraint's index leads with the definition, so it cannot serve the lookup above. This
-- index does.
CREATE INDEX idx_transition_source_event ON transition (from_state_id, event_id);
