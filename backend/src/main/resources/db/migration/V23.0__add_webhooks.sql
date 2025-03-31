CREATE TABLE IF NOT EXISTS webhook
(
    id               SERIAL PRIMARY KEY,
    url              VARCHAR(255) NOT NULL,
    ssl_verification BOOLEAN      NOT NULL DEFAULT TRUE,
    active           BOOLEAN      NOT NULL DEFAULT TRUE
);