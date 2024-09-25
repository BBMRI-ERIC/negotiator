ALTER TABLE negotiation ADD COLUMN human_readable text NOT NULL;

CREATE TABLE negotiation_resources_link (
    negotiation_id character varying(255) NOT NULL REFERENCES negotiation(id),
    resource_id bigint NOT NULL REFERENCES resource(id),
    PRIMARY KEY (negotiation_id, resource_id)
);
