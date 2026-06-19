-- Fix display_id ordering to use created_at instead of id
-- Oldest negotiation (by created_at) should have display_id = 1

WITH numbered_negotiations AS (
    SELECT id, row_number() OVER (ORDER BY creation_date ASC, id ASC) as rn
    FROM negotiation
)
UPDATE negotiation
SET display_id = numbered_negotiations.rn::text
FROM numbered_negotiations
WHERE negotiation.id = numbered_negotiations.id;

-- Reset the sequence to the correct next value
SELECT setval('negotiation_display_id_seq', GREATEST(COALESCE((SELECT MAX(display_id::bigint) FROM negotiation), 0), 1));

