CREATE TABLE admin_settings(
    id BIGINT not null,
    send_negotiations_update_notifications BOOLEAN not null
);

INSERT INTO admin_settings(id, send_negotiations_update_notifications) VALUES (1, false);