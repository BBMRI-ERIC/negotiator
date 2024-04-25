CREATE SEQUENCE value_set_id_seq START WITH 100 INCREMENT BY 50;
CREATE TABLE value_set
(
    id                     BIGSERIAL PRIMARY KEY,
    name                   VARCHAR(255),
    external_documentation VARCHAR(255)
);

CREATE TABLE value_set_available_values
(
    value_set_id     BIGINT NOT NULL,
    available_values VARCHAR(255)
);

ALTER TABLE value_set_available_values
    ADD CONSTRAINT fk_valueset_availablevalues_on_value_set FOREIGN KEY (value_set_id) REFERENCES value_set (id);