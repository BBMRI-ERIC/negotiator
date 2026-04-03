-- Local development webhook setup
INSERT INTO webhook (id, url, ssl_verification, active)
VALUES (1, 'http://localhost:3000/webhooks', true, true)
ON CONFLICT (id) DO UPDATE SET
    url = EXCLUDED.url,
    ssl_verification = EXCLUDED.ssl_verification,
    active = EXCLUDED.active;
-- Bump id sequence to avoid conflicts with future inserts
SELECT setval('webhook_id_seq', (SELECT MAX(id) FROM webhook));