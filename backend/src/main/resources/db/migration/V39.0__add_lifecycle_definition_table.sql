-- One row is one immutable Definition Version. Rows that share a family_key form a Definition
-- Family. version is a per-family display integer with no identity role, so other tables point at
-- the row id.

CREATE TABLE lifecycle_definition
(
    id                BIGSERIAL PRIMARY KEY,
    scope             VARCHAR(255) NOT NULL,
    family_key        VARCHAR(255) NOT NULL,
    name              VARCHAR(255) NOT NULL,
    version           INTEGER      NOT NULL,
    active            BOOLEAN      NOT NULL DEFAULT FALSE,
    is_global_default BOOLEAN      NOT NULL DEFAULT FALSE
);

ALTER TABLE lifecycle_definition
    ADD CONSTRAINT uq_lifecycle_definition_family_version UNIQUE (family_key, version);

-- At most one version per family is active, and new work resolves to it. A family has no active
-- version for a moment while a new version is published, so publishing checks for exactly one and
-- this index does not.
CREATE UNIQUE INDEX uq_lifecycle_definition_active_per_family
    ON lifecycle_definition (family_key)
    WHERE active;

-- The Global Default Family is the one family a Resource resolves to when nothing more specific
-- applies. The flag belongs to the family but sits on every row, so the index counts only active
-- rows. A superseded version may keep is_global_default true without a conflict.
CREATE UNIQUE INDEX uq_lifecycle_definition_global_default
    ON lifecycle_definition (is_global_default)
    WHERE is_global_default AND active;
