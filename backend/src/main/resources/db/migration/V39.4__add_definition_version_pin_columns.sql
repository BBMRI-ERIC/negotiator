-- The Definition Version Pin records the Definition Version a Lifecycle resolved to when it
-- started. The work keeps it for life, so publishing a new version never moves work already in
-- flight. A Negotiation pins one, and each of its Resources pins its own.

-- Nullable with no default, because existing rows predate the pin and there is no value to invent
-- for them.
ALTER TABLE negotiation
    ADD COLUMN lifecycle_definition_id BIGINT,
    ADD CONSTRAINT fk_negotiation_lifecycle_definition FOREIGN KEY (lifecycle_definition_id)
        REFERENCES lifecycle_definition (id) ON DELETE RESTRICT;

-- The Resource pin is per link rather than per Negotiation: two Resources of one Negotiation may
-- resolve to different Definition Families, so a Negotiation-level column could not hold it.
ALTER TABLE negotiation_resource_link
    ADD COLUMN lifecycle_definition_id BIGINT,
    ADD CONSTRAINT fk_negotiation_resource_link_lifecycle_definition
        FOREIGN KEY (lifecycle_definition_id)
        REFERENCES lifecycle_definition (id) ON DELETE RESTRICT;
