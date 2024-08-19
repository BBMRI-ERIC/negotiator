ALTER TABLE negotiation
    ALTER COLUMN payload TYPE TEXT USING (payload::TEXT);

ALTER TABLE negotiation
    ALTER COLUMN private_posts_enabled SET NOT NULL;

ALTER TABLE negotiation
    ALTER COLUMN public_posts_enabled SET NOT NULL;