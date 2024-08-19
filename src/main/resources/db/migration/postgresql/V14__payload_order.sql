ALTER TABLE negotiation
    ALTER COLUMN payload TYPE TEXT USING (payload::TEXT);
