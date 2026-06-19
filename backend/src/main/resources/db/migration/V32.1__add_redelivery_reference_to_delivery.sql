ALTER TABLE delivery
ADD COLUMN IF NOT EXISTS redelivery_of_delivery_id VARCHAR(36);

CREATE INDEX IF NOT EXISTS idx_delivery_redelivery_of
ON delivery (redelivery_of_delivery_id);
