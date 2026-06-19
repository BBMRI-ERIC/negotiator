ALTER TABLE delivery
ADD COLUMN event_type VARCHAR(64);

UPDATE delivery
SET event_type = 'CUSTOM'
WHERE event_type IS NULL;

ALTER TABLE delivery
ALTER COLUMN event_type SET NOT NULL;
