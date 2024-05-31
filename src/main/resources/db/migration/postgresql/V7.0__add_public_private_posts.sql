ALTER TABLE negotiation RENAME COLUMN posts_enabled TO public_posts_enabled;
ALTER TABLE negotiation UPDATE COLUMN public_posts_enabled SET DEFAULT true;
ALTER TABLE negotiation ADD COLUMN private_posts_enabled boolean DEFAULT false;

UPDATE negotiation n
SET public_posts_enabled = true
WHERE n.current_state in ('SUBMITTED', 'IN_PROGRESS');

UPDATE negotiation n
SET private_posts_enabled = true
WHERE n.current_state in ('IN_PROGRESS');

UPDATE negotiation n
SET private_posts_enabled = false
WHERE n.current_state IN ('SUBMITTED', 'DECLINED', 'ABANDONED');