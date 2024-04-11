CREATE TABLE negotiation_event_metadata
(
    id          bigserial NOT NULL,
    value       VARCHAR(255),
    label       VARCHAR(255),
    description VARCHAR(255),
    CONSTRAINT pk_negotiationeventmetadata PRIMARY KEY (id)
);

CREATE TABLE negotiation_state_metadata
(
    id          bigserial NOT NULL,
    value       VARCHAR(255),
    label       VARCHAR(255),
    description VARCHAR(255),
    CONSTRAINT pk_negotiationstatemetadata PRIMARY KEY (id)
);

ALTER TABLE negotiation_event_metadata
    ADD CONSTRAINT uc_negotiationeventmetadata_value UNIQUE (value);

ALTER TABLE negotiation_state_metadata
    ADD CONSTRAINT uc_negotiationstatemetadata_value UNIQUE (value);