-- The edges of a Lifecycle Definition's graph. A Transition names its definition, its from-State, its
-- to-State, its Event and its Required Authority.

-- A Transition must not straddle two Lifecycle Definitions: loading a graph by its Definition Version
-- id would otherwise return edges pointing out of it. That is expressible, as a composite foreign key
-- on (lifecycle_definition_id, id) — which PostgreSQL will only accept against a unique constraint, so
-- each vertex table gets one. Redundant as a uniqueness claim, since id alone is already the primary
-- key; its whole job is to be a referenceable pair.
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

-- The evaluator's question is "which Transition leaves State X for Event Y", and its answer must be a
-- single row: a duplicate would be an ambiguity it has no way to resolve. The frozen graph dump has 0
-- duplicate (source, event) pairs across both live graphs, so this constrains nothing real.
ALTER TABLE transition
    ADD CONSTRAINT uq_transition_definition_source_event
        UNIQUE (lifecycle_definition_id, from_state_id, event_id);

-- That constraint's index leads with the definition, so it cannot serve the lookup above. This one
-- does, and is the index the evaluator will live on.
CREATE INDEX idx_transition_source_event ON transition (from_state_id, event_id);
