-- The two vertex tables of a Lifecycle Definition's graph, both owned by one definition row.

-- A State is a named position. label is what the UI and the Resource state-change notification body
-- render, so a State without one would degrade a live notification to nulls in a sentence.
CREATE TABLE state
(
    id                      BIGSERIAL PRIMARY KEY,
    lifecycle_definition_id BIGINT       NOT NULL,
    name                    VARCHAR(255) NOT NULL,
    label                   VARCHAR(255) NOT NULL,
    initial                 BOOLEAN      NOT NULL DEFAULT FALSE,
    terminal                BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_state_lifecycle_definition FOREIGN KEY (lifecycle_definition_id)
        REFERENCES lifecycle_definition (id) ON DELETE RESTRICT
);

-- A live state string is resolved through the Definition Version Pin plus the state name, so the name
-- is a natural key and a duplicate within one definition would make that lookup ambiguous.
ALTER TABLE state
    ADD CONSTRAINT uq_state_name_per_definition UNIQUE (lifecycle_definition_id, name);

-- At most one State per definition is where a Lifecycle starts. Zero is legal here — the *at least
-- one* half is checked when a version is published, not by a row-level constraint.
CREATE UNIQUE INDEX uq_state_initial_per_definition
    ON state (lifecycle_definition_id)
    WHERE initial;

-- An Event is a named trigger. It may carry no Transition at all: the Override Event exists only as
-- the name a direct state change appears under in history, so nothing here assumes reachability.
CREATE TABLE event
(
    id                      BIGSERIAL PRIMARY KEY,
    lifecycle_definition_id BIGINT       NOT NULL,
    name                    VARCHAR(255) NOT NULL,
    CONSTRAINT fk_event_lifecycle_definition FOREIGN KEY (lifecycle_definition_id)
        REFERENCES lifecycle_definition (id) ON DELETE RESTRICT
);

-- An Information Requirement is re-homed by matching its legacy for_event string against the Event of
-- the same name, so this name is a natural key for the same reason a state name is.
ALTER TABLE event
    ADD CONSTRAINT uq_event_name_per_definition UNIQUE (lifecycle_definition_id, name);
