-- The root table of the Lifecycle Definition schema (ADR 0002, ADR 0003).
-- One row is one complete, immutable Definition Version. Rows sharing a family_key form a
-- Definition Family; version is a per-family display integer and carries no identity, so the
-- row id is the sole machine identity and nothing looks a definition up by (family_key, version).
-- Additive only: this file creates a table nothing reads yet, and is safe to deploy on its own.

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

-- A version number is unique within its family. Display-only, but a family that shows two "v3"s
-- would be lying about its own history.
ALTER TABLE lifecycle_definition
    ADD CONSTRAINT uq_lifecycle_definition_family_version UNIQUE (family_key, version);

-- At most one version per family is active: publishing is a one-step flip of this flag, and two
-- active versions would leave new work with no answer to which one it resolves to.
-- ADR 0003 says *exactly* one, and the "at least one" half is deliberately not enforced here: a
-- family with zero active versions is a valid intermediate state during any publish, so it belongs
-- to publish-time validation (stage 3), not to a row-level constraint.
CREATE UNIQUE INDEX uq_lifecycle_definition_active_per_family
    ON lifecycle_definition (family_key)
    WHERE active;

-- At most one active version in the whole table is the Global Default. Scoped to active rows
-- because the flag is a fact about the *family* and travels with it across versions: a superseded
-- version keeps its true, which is honest — it was the default while it was active.
CREATE UNIQUE INDEX uq_lifecycle_definition_global_default
    ON lifecycle_definition (is_global_default)
    WHERE is_global_default AND active;

-- Two further invariants are known to be unenforced here, both for the same reason as the
-- "at least one active version" half above — they are not row-level facts, and publish-time
-- validation in stage 3 is where they belong:
--   * scope is fixed for a whole Definition Family (backend/CONTEXT.md:27) but is stored per row,
--     and there is no family table to hold it, so rows of one family can disagree.
--   * a Definition Version needs at least one initial State.
-- Do not close either with a trigger or a deferred constraint.

-- Note for later slices: foreign keys pointing at this table are ON DELETE RESTRICT. ADR 0003 says
-- a version that is active or referenced is never mutated in place and never discarded, so a
-- cascade would express a deletion the model does not have.
