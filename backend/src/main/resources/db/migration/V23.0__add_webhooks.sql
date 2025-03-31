CREATE TABLE IF NOT EXISTS webhook
(
    id               SERIAL PRIMARY KEY,
    url              VARCHAR(255) NOT NULL,
    ssl_verification BOOLEAN      NOT NULL DEFAULT TRUE,
    active           BOOLEAN      NOT NULL DEFAULT TRUE
);

-- Table for deliveries
CREATE TABLE delivery
(
    id               VARCHAR(36) PRIMARY KEY,
    webhook_id       BIGINT    NOT NULL,
    successful       BOOLEAN   NOT NULL DEFAULT FALSE,
    content          JSON      NOT NULL,
    http_status_code INT,
    error_message    VARCHAR(255),
    at               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_webhook FOREIGN KEY (webhook_id) REFERENCES webhook (id) ON DELETE CASCADE
);