ALTER TABLE delivery
ADD COLUMN redelivery_of_delivery_id VARCHAR(36);

ALTER TABLE delivery
ADD CONSTRAINT fk_delivery_redelivery_of
FOREIGN KEY (redelivery_of_delivery_id)
REFERENCES delivery (id)
ON DELETE SET NULL;

CREATE INDEX idx_delivery_redelivery_of
ON delivery (redelivery_of_delivery_id);
