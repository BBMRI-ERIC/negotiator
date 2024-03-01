ALTER TABLE access_criteria_section_link
    DROP CONSTRAINT access_criteria_section_link_created_by_fkey;

ALTER TABLE access_criteria_section_link
    DROP CONSTRAINT access_criteria_section_link_modified_by_fkey;

ALTER TABLE access_criteria_section_link
    DROP CONSTRAINT fkcgj2extn02c91q2ld1xvp54di;

ALTER TABLE access_criteria_section_link
    DROP CONSTRAINT fkhwqk0a4nxt3p18e97l4llg4bs;

ALTER TABLE notification
    DROP CONSTRAINT notification_created_by_fkey;

ALTER TABLE notification
    DROP CONSTRAINT notification_modified_by_fkey;

CREATE SEQUENCE IF NOT EXISTS access_form_element_id_seq START WITH 100 INCREMENT BY 50;

CREATE SEQUENCE IF NOT EXISTS access_form_id_seq START WITH 100 INCREMENT BY 50;

CREATE SEQUENCE IF NOT EXISTS access_form_section_id_seq START WITH 100 INCREMENT BY 50;

CREATE SEQUENCE IF NOT EXISTS access_form_section_link_id_seq START WITH 100 INCREMENT BY 50;

CREATE SEQUENCE IF NOT EXISTS section_element_link_id_seq START WITH 100 INCREMENT BY 50;

ALTER TABLE access_criteria_set
    RENAME TO access_form;

ALTER TABLE access_criteria
    RENAME TO access_form_element;

ALTER TABLE access_criteria_section
    RENAME TO access_form_section;

CREATE TABLE access_form_section_element_link
(
    id                          BIGINT  NOT NULL,
    access_form_section_link_id BIGINT,
    access_form_element_id      BIGINT,
    is_required                 BOOLEAN NOT NULL,
    element_order               INTEGER NOT NULL,
    CONSTRAINT pk_accessformsectionelementlink PRIMARY KEY (id)
);

CREATE TABLE access_form_section_link
(
    id                     BIGINT  NOT NULL,
    access_form_id         BIGINT,
    access_form_section_id BIGINT,
    section_order          INTEGER NOT NULL,
    CONSTRAINT pk_accessformsectionlink PRIMARY KEY (id)
);

ALTER TABLE resource
    RENAME COLUMN access_criteria_set_id TO access_form_id;

ALTER TABLE access_form_section_link
    ADD CONSTRAINT uc_accessformsectionlink_acfoidacfoseid UNIQUE (access_form_id, access_form_section_id);

ALTER TABLE access_form_section_element_link
    ADD CONSTRAINT FK_ACCESSFORMSECTIONELEMENTLINK_ON_ACCESS_FORM_SECTION_LINK FOREIGN KEY (access_form_section_link_id) REFERENCES access_form_section_link (id);

DROP TABLE access_criteria_section_link CASCADE;