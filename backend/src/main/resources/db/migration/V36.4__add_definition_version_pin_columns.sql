-- The Definition Version Pin: the Lifecycle Definition a piece of work resolved to when its
-- Lifecycle started, and which it then keeps for life so that publishing a new version never moves
-- work already in flight. Negotiations pin one, and each of a Negotiation's Resources pins its own.

-- Nullable, and with no default. Every existing row predates the pin and there is no value to
-- invent for it, so the column is added empty; the data cutover backfills it and only then sets
-- NOT NULL. That keeps this file additive and safe to land on its own.
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
