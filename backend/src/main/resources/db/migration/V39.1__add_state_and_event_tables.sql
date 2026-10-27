-- The UI and the Resource state-change notification body render label, so a State without one
-- would put nulls into a live notification.
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

-- A live state string resolves through the Definition Version Pin and the state name, so the name
-- is a natural key. A duplicate within one definition would make that lookup ambiguous.
ALTER TABLE state
    ADD CONSTRAINT uq_state_name_per_definition UNIQUE (lifecycle_definition_id, name);

-- At most one State per definition is initial. Publishing a version checks that there is at least
-- one, because a row-level constraint cannot.
CREATE UNIQUE INDEX uq_state_initial_per_definition
    ON state (lifecycle_definition_id)
    WHERE initial;

-- An Event may have no Transition. The Override Event exists only as the name a direct state change
-- appears under in history, so nothing here assumes an Event is reachable.
CREATE TABLE event
(
    id                      BIGSERIAL PRIMARY KEY,
    lifecycle_definition_id BIGINT       NOT NULL,
    name                    VARCHAR(255) NOT NULL,
    CONSTRAINT fk_event_lifecycle_definition FOREIGN KEY (lifecycle_definition_id)
        REFERENCES lifecycle_definition (id) ON DELETE RESTRICT
);

-- An Information Requirement finds its Event by matching its legacy for_event string against the
-- Event name, so the name is a natural key for the same reason a state name is.
ALTER TABLE event
    ADD CONSTRAINT uq_event_name_per_definition UNIQUE (lifecycle_definition_id, name);
