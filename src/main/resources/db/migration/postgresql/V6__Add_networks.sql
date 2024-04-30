CREATE TABLE network
(
    id            BIGINT       NOT NULL,
    uri           VARCHAR(255) NOT NULL,
    name          VARCHAR(255),
    external_id   VARCHAR(255) NOT NULL,
    contact_email VARCHAR(255),
    CONSTRAINT pk_network PRIMARY KEY (id)
);

ALTER TABLE organization
    ADD network_id BIGINT;

ALTER TABLE person
    ADD network_id BIGINT;

ALTER TABLE network
    ADD CONSTRAINT uc_network_externalid UNIQUE (external_id);

ALTER TABLE network
    ADD CONSTRAINT uc_network_name UNIQUE (name);

ALTER TABLE organization
    ADD CONSTRAINT FK_ORGANIZATION_ON_NETWORK FOREIGN KEY (network_id) REFERENCES network (id);

ALTER TABLE person
    ADD CONSTRAINT FK_PERSON_ON_NETWORK FOREIGN KEY (network_id) REFERENCES network (id);
