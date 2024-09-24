ALTER TABLE negotiation
    ADD COLUMN human_readable TEXT NOT NULL;

create TABLE negotiation_resources_link (
    negotiation_id character varying(255) NOT NULL,
    resource_id bigint NOT NULL
);
