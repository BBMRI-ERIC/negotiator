CREATE TABLE network
(
    id            BIGINT       NOT NULL,
    uri           VARCHAR(255) NOT NULL,
    name          VARCHAR(255),
    external_id   VARCHAR(255) NOT NULL,
    contact_email VARCHAR(255),
    CONSTRAINT pk_network PRIMARY KEY (id)
);


ALTER TABLE network
    ADD CONSTRAINT uc_network_externalid UNIQUE (external_id);

ALTER TABLE network
    ADD CONSTRAINT uc_network_name UNIQUE (name);


CREATE TABLE network_resources_link
(
    network_id  BIGINT NOT NULL,
    resource_id BIGINT NOT NULL,
    CONSTRAINT pk_network_resources_link PRIMARY KEY (network_id, resource_id)
);

ALTER TABLE network_resources_link
    ADD CONSTRAINT fk_netres_on_network FOREIGN KEY (network_id) REFERENCES network (id);

ALTER TABLE network_resources_link
    ADD CONSTRAINT fk_netres_on_resource FOREIGN KEY (resource_id) REFERENCES resource (id);

CREATE TABLE network_person_link
(
    network_id  BIGINT NOT NULL,
    person_id BIGINT NOT NULL,
    CONSTRAINT pk_network_person_link PRIMARY KEY (network_id, person_id)
);

ALTER TABLE network_person_link
    ADD CONSTRAINT fk_netper_on_network FOREIGN KEY (network_id) REFERENCES network (id);

ALTER TABLE network_person_link
    ADD CONSTRAINT fk_netper_on_person FOREIGN KEY (person_id) REFERENCES person (id);

