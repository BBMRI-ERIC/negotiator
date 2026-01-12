alter table negotiation
    add column display_id varchar(255);

create sequence negotiation_display_id_seq
    increment by 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

WITH numbered_negotiations AS (
    SELECT id, row_number() OVER (ORDER BY id) as rn
    FROM negotiation
)

UPDATE negotiation
SET display_id = numbered_negotiations.rn::text
FROM numbered_negotiations
WHERE negotiation.id = numbered_negotiations.id;

SELECT setval('negotiation_display_id_seq', GREATEST(COALESCE((SELECT COUNT(*) FROM negotiation), 0), 1));

alter table negotiation
    alter column display_id set default nextval('negotiation_display_id_seq')::text;

