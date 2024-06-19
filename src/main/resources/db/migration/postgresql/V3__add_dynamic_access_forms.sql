CREATE SEQUENCE access_form_element_id_seq START WITH 100 INCREMENT BY 50;

CREATE SEQUENCE access_form_id_seq START WITH 100 INCREMENT BY 50;

CREATE SEQUENCE access_form_section_id_seq START WITH 100 INCREMENT BY 50;

CREATE SEQUENCE access_form_section_link_id_seq START WITH 100 INCREMENT BY 50;

CREATE SEQUENCE section_element_link_id_seq START WITH 100 INCREMENT BY 50;

CREATE TABLE access_form
(
    id            BIGINT NOT NULL,
    creation_date TIMESTAMP,
    modified_date TIMESTAMP,
    created_by    BIGINT,
    modified_by   BIGINT,
    name          VARCHAR(255),
    CONSTRAINT pk_accessform PRIMARY KEY (id)
);

CREATE TABLE access_form_element
(
    id                     BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    creation_date          TIMESTAMP,
    modified_date          TIMESTAMP,
    created_by             BIGINT,
    modified_by            BIGINT,
    name                   VARCHAR(255)                            NOT NULL,
    label                  VARCHAR(255)                            NOT NULL,
    description            VARCHAR(255)                            NOT NULL,
    type                   VARCHAR(255)                            NOT NULL,
    access_form_section_id BIGINT,
    CONSTRAINT pk_accessformelement PRIMARY KEY (id)
);

CREATE TABLE access_form_section
(
    id            BIGINT       NOT NULL,
    creation_date TIMESTAMP,
    modified_date TIMESTAMP,
    created_by    BIGINT,
    modified_by   BIGINT,
    name          VARCHAR(255) NOT NULL,
    label         VARCHAR(255) NOT NULL,
    description   VARCHAR(255) NOT NULL,
    CONSTRAINT pk_accessformsection PRIMARY KEY (id)
);

CREATE TABLE access_form_section_element_link
(
    id                          BIGINT  NOT NULL,
    access_form_section_link_id BIGINT,
    access_form_element_id      BIGINT,
    is_required                 BOOLEAN NOT NULL,
    element_order               INT     NOT NULL,
    CONSTRAINT pk_accessformsectionelementlink PRIMARY KEY (id)
);

CREATE TABLE access_form_section_link
(
    id                     BIGINT NOT NULL,
    access_form_id         BIGINT,
    access_form_section_id BIGINT,
    section_order          INT    NOT NULL,
    CONSTRAINT pk_accessformsectionlink PRIMARY KEY (id)
);

DROP TABLE ACCESS_CRITERIA CASCADE;

DROP TABLE ACCESS_CRITERIA_SECTION CASCADE;

DROP TABLE ACCESS_CRITERIA_SECTION_LINK CASCADE;

DROP TABLE ACCESS_CRITERIA_SET CASCADE;

ALTER TABLE resource
    ADD access_form_id BIGINT;

ALTER TABLE access_form_section_link
    ADD CONSTRAINT uc_accessformsectionlink_acfoidacfoseid UNIQUE (access_form_id, access_form_section_id);

ALTER TABLE access_form_element
    ADD CONSTRAINT FK_ACCESSFORMELEMENT_ON_ACCESS_FORM_SECTION FOREIGN KEY (access_form_section_id) REFERENCES access_form_section (id);

ALTER TABLE access_form_element
    ADD CONSTRAINT FK_ACCESSFORMELEMENT_ON_CREATED_BY FOREIGN KEY (created_by) REFERENCES person (id);

ALTER TABLE access_form_element
    ADD CONSTRAINT FK_ACCESSFORMELEMENT_ON_MODIFIED_BY FOREIGN KEY (modified_by) REFERENCES person (id);

ALTER TABLE access_form_section_element_link
    ADD CONSTRAINT FK_ACCESSFORMSECTIONELEMENTLINK_ON_ACCESS_FORM_ELEMENT FOREIGN KEY (access_form_element_id) REFERENCES access_form_element (id);

ALTER TABLE access_form_section_element_link
    ADD CONSTRAINT FK_ACCESSFORMSECTIONELEMENTLINK_ON_ACCESS_FORM_SECTION_LINK FOREIGN KEY (access_form_section_link_id) REFERENCES access_form_section_link (id);

ALTER TABLE access_form_section_link
    ADD CONSTRAINT FK_ACCESSFORMSECTIONLINK_ON_ACCESS_FORM FOREIGN KEY (access_form_id) REFERENCES access_form (id);

ALTER TABLE access_form_section_link
    ADD CONSTRAINT FK_ACCESSFORMSECTIONLINK_ON_ACCESS_FORM_SECTION FOREIGN KEY (access_form_section_id) REFERENCES access_form_section (id);

ALTER TABLE access_form_section
    ADD CONSTRAINT FK_ACCESSFORMSECTION_ON_CREATED_BY FOREIGN KEY (created_by) REFERENCES person (id);

ALTER TABLE access_form_section
    ADD CONSTRAINT FK_ACCESSFORMSECTION_ON_MODIFIED_BY FOREIGN KEY (modified_by) REFERENCES person (id);

ALTER TABLE access_form
    ADD CONSTRAINT FK_ACCESSFORM_ON_CREATED_BY FOREIGN KEY (created_by) REFERENCES person (id);

ALTER TABLE access_form
    ADD CONSTRAINT FK_ACCESSFORM_ON_MODIFIED_BY FOREIGN KEY (modified_by) REFERENCES person (id);

ALTER TABLE resource
    ADD CONSTRAINT FK_RESOURCE_ON_ACCESS_FORM FOREIGN KEY (access_form_id) REFERENCES access_form (id);

ALTER TABLE resource
    drop column ACCESS_CRITERIA_SET_ID;

insert into access_form (id, name)
values (1, 'BBMRI Template'),
       (2, 'BBMRI.de Template'),
       (3, 'BBMRI.cz Template')
ON CONFLICT DO NOTHING;
insert into access_form_section (id, name, label, description)
VALUES (1, 'project', 'Project', 'Provide information about your project'),
       (2, 'request', 'Request', 'Provide information the resources you are requesting'),
       (3, 'ethics-vote', 'Ethics vote', 'Is ethics vote present in your project?')
ON CONFLICT DO NOTHING;


INSERT INTO access_form_element (id, name, label, description, type)
VALUES (1, 'title', 'Title', 'Give a title', 'text'),
       (2, 'description', 'Description', 'Give a description', 'textarea'),
       (3, 'description', 'Description', 'Provide a request description', 'textarea'),
       (4, 'ethics-vote', 'Ethics vote', 'Write the etchics vote', 'textarea'),
       (5, 'ethics-vote-attachment', 'Attachment', 'Upload Ethics Vote', 'file'),
       (6, 'objective', 'Study objective', 'Study objective or hypothesis to be tested?', 'text'),
       (7, 'profit', 'Profit', 'Is it a profit or a non-profit study', 'boolean'),
       (8, 'acknowledgment', 'Acknowledgment', 'Financing/ Acknowledgement or collaboration of the collection PIs?',
        'text'),
       (9, 'disease-code', 'Disease code', 'What is the Disease being studied (ICD 10 code) ?', 'text'),
       (10, 'collection', 'Collection', 'Is the collection to be prospective or retrospective?', 'text'),
       (11, 'donors', 'Donors', 'How many different subjects (donors) would you need?', 'number'),
       (12, 'samples', 'Samples', 'What type(s) of samples and how many samples per subject are needed?', 'text'),
       (13, 'specifics', 'Specifics', 'Are there any specific requirements ( e.g. volume,… )?', 'text'),
       (14, 'organization', 'Organization', 'What is the organization leading this project and where is it based',
        'text')
ON CONFLICT DO NOTHING;

INSERT INTO ACCESS_FORM_SECTION_LINK (ID, ACCESS_FORM_ID, ACCESS_FORM_SECTION_ID, SECTION_ORDER)
VALUES (1, 1, 1, 0),
       (2, 1, 2, 1),
       (3, 1, 3, 2),
       (4, 2, 1, 0),
       (5, 2, 2, 1),
       (6, 2, 3, 2),
       (7, 3, 1, 0),
       (8, 3, 2, 1),
       (9, 3, 3, 2)
ON CONFLICT DO NOTHING;

INSERT INTO ACCESS_FORM_SECTION_ELEMENT_LINK (ID, ACCESS_FORM_SECTION_LINK_ID, ACCESS_FORM_ELEMENT_ID, IS_REQUIRED,
                                              ELEMENT_ORDER)
VALUES (1, 1, 1, true, 1),
       (2, 1, 2, true, 2),
       (3, 2, 3, true, 1),
       (4, 3, 4, true, 1),
       (5, 3, 5, false, 2),
       (6, 4, 1, true, 1),
       (7, 4, 2, true, 2),
       (8, 5, 3, true, 1),
       (9, 6, 4, true, 1),
       (10, 6, 5, false, 2),
       (11, 4, 6, true, 3),
       (12, 4, 7, true, 5),
       (13, 4, 8, true, 6),
       (14, 5, 9, false, 2),
       (15, 5, 10, false, 3),
       (16, 5, 11, true, 4),
       (17, 5, 12, true, 5),
       (18, 5, 13, false, 6),
       ---
       (19, 7, 1, true, 1),
       (20, 7, 2, true, 2),
       (21, 8, 3, true, 1),
       (22, 9, 4, true, 1),
       (23, 9, 5, false, 2),
       (24, 7, 6, true, 3),
       (25, 7, 7, true, 5),
       (26, 7, 8, true, 6),
       (27, 7, 9, false, 2),
       (28, 8, 10, false, 3),
       (29, 8, 11, true, 4),
       (30, 8, 12, true, 5),
       (31, 8, 13, false, 6),
       (32, 7, 14, false, 4)
ON CONFLICT DO NOTHING;
update resource
SET access_form_id = 2
where source_id like '%:DE%';
update resource
SET access_form_id = 3
where source_id like '%:CZ%';
update resource
set access_form_id = 1
where RESOURCE.access_form_id is null;
