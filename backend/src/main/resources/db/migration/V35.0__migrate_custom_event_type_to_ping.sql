UPDATE webhook_delivery
SET event_type = 'PING'
WHERE event_type = 'CUSTOM';
