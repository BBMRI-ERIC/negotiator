-- 1. Add a new column of type TEXT
ALTER TABLE negotiation
    ADD COLUMN payload_text TEXT;

-- 2. Copy the data from the JSONB column to the new TEXT column
UPDATE negotiation
SET payload_text = payload::TEXT;

-- 3. Drop the original JSONB column
ALTER TABLE negotiation
    DROP COLUMN payload;

-- 4. Rename the new TEXT column to the original column name
ALTER TABLE negotiation
    RENAME COLUMN payload_text TO payload;

ALTER TABLE information_submission
    ADD COLUMN payload_text TEXT;

-- 2. Copy the data from the JSONB column to the new TEXT column
UPDATE information_submission
SET payload_text = payload::TEXT;

-- 3. Drop the original JSONB column
ALTER TABLE information_submission
    DROP COLUMN payload;

-- 4. Rename the new TEXT column to the original column name
ALTER TABLE information_submission
    RENAME COLUMN payload_text TO payload;
