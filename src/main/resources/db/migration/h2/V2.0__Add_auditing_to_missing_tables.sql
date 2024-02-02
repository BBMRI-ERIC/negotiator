ALTER TABLE access_criteria ADD creation_date timestamp(6);
ALTER TABLE access_criteria ADD modified_date timestamp(6);
ALTER TABLE access_criteria ADD created_by bigint REFERENCES person(id);
ALTER TABLE access_criteria ADD modified_by bigint REFERENCES person(id);

ALTER TABLE access_criteria_section ADD creation_date timestamp(6);
ALTER TABLE access_criteria_section ADD modified_date timestamp(6);
ALTER TABLE access_criteria_section ADD created_by bigint REFERENCES person(id);
ALTER TABLE access_criteria_section ADD modified_by bigint REFERENCES person(id);

ALTER TABLE access_criteria_section_link ADD creation_date timestamp(6);
ALTER TABLE access_criteria_section_link ADD modified_date timestamp(6);
ALTER TABLE access_criteria_section_link ADD created_by bigint REFERENCES person(id);
ALTER TABLE access_criteria_section_link ADD modified_by bigint REFERENCES person(id);

ALTER TABLE access_criteria_set ADD creation_date timestamp(6);
ALTER TABLE access_criteria_set ADD modified_date timestamp(6);
ALTER TABLE access_criteria_set ADD created_by bigint REFERENCES person(id);
ALTER TABLE access_criteria_set ADD modified_by bigint REFERENCES person(id);

ALTER TABLE data_source ADD creation_date timestamp(6);
ALTER TABLE data_source ADD modified_date timestamp(6);
ALTER TABLE data_source ADD created_by bigint REFERENCES person(id);
ALTER TABLE data_source ADD modified_by bigint REFERENCES person(id);

ALTER TABLE negotiation_lifecycle_record RENAME COLUMN recorded_at TO creation_date;

ALTER TABLE negotiation_lifecycle_record ADD modified_date timestamp(6);
ALTER TABLE negotiation_lifecycle_record ADD created_by bigint REFERENCES person(id);
ALTER TABLE negotiation_lifecycle_record ADD modified_by bigint REFERENCES person(id);

ALTER TABLE notification ADD modified_date timestamp(6);
ALTER TABLE notification ADD created_by bigint REFERENCES person(id);
ALTER TABLE notification ADD modified_by bigint REFERENCES person(id);

ALTER TABLE notification_email RENAME COLUMN sent_at TO creation_date;
ALTER TABLE notification_email ADD modified_date timestamp(6);
ALTER TABLE notification_email ADD created_by bigint REFERENCES person(id);
ALTER TABLE notification_email ADD modified_by bigint REFERENCES person(id);

ALTER TABLE organization ADD creation_date timestamp(6);
ALTER TABLE organization ADD modified_date timestamp(6);
ALTER TABLE organization ADD created_by bigint REFERENCES person(id);
ALTER TABLE organization ADD modified_by bigint REFERENCES person(id);

ALTER TABLE person ADD creation_date timestamp(6);
ALTER TABLE person ADD modified_date timestamp(6);
ALTER TABLE person ADD created_by bigint REFERENCES person(id);
ALTER TABLE person ADD modified_by bigint REFERENCES person(id);

ALTER TABLE person_negotiation_role ADD creation_date timestamp(6);
ALTER TABLE person_negotiation_role ADD modified_date timestamp(6);
ALTER TABLE person_negotiation_role ADD created_by bigint REFERENCES person(id);
ALTER TABLE person_negotiation_role ADD modified_by bigint REFERENCES person(id);

ALTER TABLE person_project_role ADD creation_date timestamp(6);
ALTER TABLE person_project_role ADD modified_date timestamp(6);
ALTER TABLE person_project_role ADD created_by bigint REFERENCES person(id);
ALTER TABLE person_project_role ADD modified_by bigint REFERENCES person(id);