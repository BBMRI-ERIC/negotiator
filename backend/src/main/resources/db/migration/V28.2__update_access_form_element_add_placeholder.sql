ALTER TABLE access_form_element add column placeholder VARCHAR(255);
-- Set placeholder to description for existing records
UPDATE access_form_element SET placeholder = description;