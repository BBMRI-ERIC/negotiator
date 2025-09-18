ALTER TABLE access_form_element add column placeholder VARCHAR(255);

INSERT INTO access_form_element (id, name, label, description, type, placeholder)
VALUES (1, 'title', 'Title', 'Give a title', 'TEXT', 'Give a title'),
       (2, 'description', 'Description', 'Give a description', 'TEXT_LARGE','Give a description'),
       (3, 'description', 'Description', 'Provide a request description', 'TEXT_LARGE', 'Provide a request description'),
       (4, 'ethics-vote', 'Ethics vote', 'Write the etchics vote', 'TEXT_LARGE', 'Write the etchics vote'),
       (5, 'ethics-vote-attachment', 'Attachment', 'Upload Ethics Vote', 'FILE', 'Upload Ethics Vote'),
       (6, 'objective', 'Study objective', 'Study objective or hypothesis to be tested?', 'TEXT', 'Study objective or hypothesis to be tested?'),
       (7, 'profit', 'Profit', 'Is it a profit or a non-profit study', 'BOOLEAN', 'Is it a profit or a non-profit study'),
       (8, 'acknowledgment', 'Acknowledgment', 'Financing/ Acknowledgement or collaboration of the collection PIs?',
        'TEXT', 'Financing/ Acknowledgement or collaboration of the collection PIs?'),
       (9, 'disease-code', 'Disease code', 'What is the Disease being studied (ICD 10 code) ?', 'TEXT', 'What is the Disease being studied (ICD 10 code) ?'),
       (10, 'collection', 'Collection', 'Is the collection to be prospective or retrospective?', 'TEXT', 'Is the collection to be prospective or retrospective?'),
       (11, 'donors', 'Donors', 'How many different subjects (donors) would you need?', 'NUMBER', 'How many different subjects (donors) would you need?'),
       (12, 'samples', 'Samples', 'What type(s) of samples and how many samples per subject are needed?', 'TEXT', 'What type(s) of samples and how many samples per subject are needed?'),
       (13, 'specifics', 'Specifics', 'Are there any specific requirements ( e.g. volume,… )?', 'TEXT', 'Are there any specific requirements ( e.g. volume,… )?'),
       (14, 'organization', 'Organization', 'What is the organization leading this project and where is it based',
        'TEXT', 'What is the organization leading this project and where is it based')
ON CONFLICT(id) DO UPDATE SET placeholder = EXCLUDED.placeholder;