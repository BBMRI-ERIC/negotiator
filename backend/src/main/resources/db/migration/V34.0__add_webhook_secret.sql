CREATE TABLE webhook_secret (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    encrypted_secret TEXT NOT NULL,
    salt VARCHAR(64) NOT NULL
);

ALTER TABLE webhook
ADD COLUMN secret_id VARCHAR(36);

ALTER TABLE webhook
ADD CONSTRAINT fk_webhook_secret
FOREIGN KEY (secret_id) REFERENCES webhook_secret(id);
