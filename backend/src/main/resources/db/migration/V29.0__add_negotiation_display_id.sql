-- Add display_name column to negotiation table
ALTER TABLE negotiation
ADD COLUMN display_id VARCHAR(255);

-- Set initial display_id from title for existing records
UPDATE negotiation
SET display_id = JSON_EXTRACT_PATH_TEXT(payload, 'project', 'display_id')
WHERE display_id IS NULL AND payload IS NOT NULL;

-- For negotiations without a title in payload, set display_id to a default value
UPDATE negotiation
SET display_id = 'NEG-' || SUBSTRING(id FROM 1 FOR 8)
WHERE display_id IS NULL OR display_id = '';