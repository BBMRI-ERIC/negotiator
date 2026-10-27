-- One row is one immutable Definition Version. Rows sharing a family_key form a Definition Family;
-- version is a per-family display integer and carries no identity, so the row id is what everything
-- else points at.

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

-- A version number is unique within its family.
ALTER TABLE lifecycle_definition
    ADD CONSTRAINT uq_lifecycle_definition_family_version UNIQUE (family_key, version);

-- At most one version per family is active: the one new work resolves to. Zero active versions is
-- legal — a family passes through it while a new version is being published — so "exactly one" will
-- be checked when a version is published, not here.
CREATE UNIQUE INDEX uq_lifecycle_definition_active_per_family
    ON lifecycle_definition (family_key)
    WHERE active;

-- The Global Default Family is the one family a Resource resolves to when nothing more specific
-- applies. The flag belongs to the family but is stored per row, so the index is scoped to active
-- rows: a superseded version may keep is_global_default true without contesting the flag.
CREATE UNIQUE INDEX uq_lifecycle_definition_global_default
    ON lifecycle_definition (is_global_default)
    WHERE is_global_default AND active;
