alter table notification
    drop column email_status,
    drop column modified_date,
    drop column modified_by,
    drop column created_by,
    add column read BOOLEAN default false;
alter table notification
    rename column creation_date to created_at;
alter table notification_email
    drop column was_successfully_sent;
alter table notification_email
    add column address TEXT;
UPDATE notification_email
SET address = p.email
FROM person p
WHERE notification_email.person_id = p.id;
ALTER TABLE notification_email
    DROP COLUMN person_id;