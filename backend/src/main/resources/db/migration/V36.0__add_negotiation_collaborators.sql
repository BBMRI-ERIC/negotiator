CREATE TABLE negotiator_collaborator_link
(
    negotiation_id   VARCHAR(255) NOT NULL,
    person_id BIGINT       NOT NULL,
    CONSTRAINT pk_negotiator_collaborator_link PRIMARY KEY (negotiation_id, person_id)
);

ALTER TABLE negotiator_collaborator_link
    ADD CONSTRAINT fk_negcol_on_negotiation FOREIGN KEY (negotiation_id) REFERENCES negotiation (id);

ALTER TABLE negotiator_collaborator_link
    ADD CONSTRAINT fk_negcol_on_person FOREIGN KEY (person_id) REFERENCES person (id);
