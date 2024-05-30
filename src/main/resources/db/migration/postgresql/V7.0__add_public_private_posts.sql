ALTER TABLE negotiation RENAME COLUMN posts_enabled TO public_posts_enabled;

ALTER TABLE negotiation ADD COLUMN private_posts_enabled boolean;

UPDATE negotiation n
SET public_posts_enabled = true
WHERE n.current_state in ('SUBMITTED', 'APPROVED', 'DECLINED', 'IN_PROGRESS', 'PAUSED', 'CONCLUDED');

UPDATE negotiation n
SET private_posts_enabled = true
WHERE n.current_state in ('APPROVED', 'IN_PROGRESS', 'PAUSED', 'CONCLUDED');

UPDATE negotiation n
SET private_posts_enabled = false
WHERE n.current_state IN ('SUBMITTED', 'DECLINED', 'ABANDONED');