insert into role (id, name)
values (1, 'CREATOR'),
       (2, 'ADMINISTRATOR'),
       (3, 'MANAGER'),
       (4, 'ROLE_RESEARCHER'),
       (5, 'REPRESENTATIVE');

insert into data_source (url, api_username, api_password, api_type, api_url, description, name,
                         resource_biobank, resource_collection, resource_network, source_prefix, sync_active)
values ('http://datasource.dev', 'user', 'password', 'MOLGENIS', 'http://datasource.dev',
        'Biobank Directory', 'Biobank Directory', 'directory_biobanks', 'directory_collections',
        'directory_networks', 'source_prefix', 'false');

insert into person (id, email, name, subject_id, password, organization, admin)
values (101, 'admin@negotiator.dev', 'admin', '1', '$2a$10$Kk29y.f7WeQeyym0X7YnvewDm3Gm/puTWGFniJvWen93C/f/6Bqey',
        'BBMRI', true),
       (102, 'directory@negotiator.dev', 'directory', '2',
        '$2a$10$iHi5bQ8nTRRF1bkiJfygkONgmABH1xNpLy2MZrHdusP.7.Rjpwk.i', 'BBMRI', false),
       (103, 'perun@negotiator.dev', 'perun', '3', '$2a$10$RCBPPd3suXNB4vLSowDdUe5umkyZaDJCt.8DtG3xVidUhxWe2Woci',
        'BBMRI', false),
       (104, 'researcher@negotiator.dev', 'researcher', '4',
        '$2a$10$6Rc4eC5vo2IMGP0KUgrxIObq2SQoHTBKx8/o/Eyq1PpmzdBtTKj0u', 'BBMRI', false),
       (105, 'sarah.representative@gmail.com', 'SarahRepr', '5', null, 'Test Biobank', false),
       (108, 'adam.researcher@gmail.com', 'TheResearcher', '1000@bbmri.eu', null, 'BBMRI', false),
       (109, 'taylor.biobanker@gmail.com', 'TheBiobanker', '1001@bbmri.eu', null, 'BBMRI', false);

insert into authorities (person_id, authority)
values (101, 'ADMIN'),
       (108, 'RESEARCHER'),  -- TheResearcher has only RESEARCHER authority
       (105, 'RESEARCHER'),  -- SarahRepr has both RESEARCHER and REPRESENTATIVE authority
       (105, 'ROLE_REPRESENTATIVE_biobank:3:collection:1'),
       (105, 'ROLE_REPRESENTATIVE_biobank:3:collection:2'),
       (105, 'ROLE_REPRESENTATIVE_biobank:3:collection:3'),
       (109, 'ROLE_REPRESENTATIVE_biobank:1:collection:1'),  -- TheBiobanker has only REPRESENTATIVE authority
       (109, 'ROLE_REPRESENTATIVE_biobank:1:collection:2'),
       (109, 'ROLE_REPRESENTATIVE_biobank:2:collection:1');

insert into access_form (id, name)
values (1, 'BBMRI Template');
insert into access_form_section (id, name, label, description)
values (1, 'project', 'Project', 'Provide information about your project');
insert into access_form_section (id, name, label, description)
values (2, 'request', 'Request', 'Provide information the resources you are requesting');
insert into access_form_section (id, name, label, description)
values (3, 'ethics-vote', 'Ethics vote', 'Is ethics vote present in your project?');

insert into access_form_element (id, name, label, description, type, access_form_section_id)
values (1, 'title', 'Title', 'Give a title', 'text', 1),
       (2, 'description', 'Description', 'Give a description', 'textarea', 1),
       (3, 'description', 'Description', 'Provide a request description', 'textarea', 2),
       (4, 'ethics-vote', 'Ethics vote', 'Write the etchics vote', 'textarea', 3),
       (5, 'ethics-vote-attachment', 'Attachment', 'Upload Ethics Vote', 'file', 3);

INSERT INTO ACCESS_FORM_SECTION_LINK (ID, ACCESS_FORM_ID, ACCESS_FORM_SECTION_ID, SECTION_ORDER)
VALUES (1, 1, 1, 0),
       (2, 1, 2, 1),
       (3, 1, 3, 2);
INSERT INTO ACCESS_FORM_SECTION_ELEMENT_LINK (ID, ACCESS_FORM_SECTION_LINK_ID, ACCESS_FORM_ELEMENT_ID, IS_REQUIRED,
                                              ELEMENT_ORDER)
VALUES (1, 1, 1, true, 1),
       (2, 1, 2, true, 2),
       (3, 2, 3, true, 1),
       (4, 3, 4, true, 1),
       (5, 3, 5, false, 2);

insert into organization (id, name, external_id)
values (4, 'Biobank #1', 'biobank:1'),
       (5, 'Biobank #2', 'biobank:2'),
       (6, 'Biobank #3', 'biobank:3');

insert into resource (id, name, description, source_id, data_source_id, organization_id, access_form_id)
values (4, 'Test collection #1 of biobank #1', 'This is the first test collection of biobank 1',
        'biobank:1:collection:1', 1, 4, 1),
       (5, 'Test collection #2 of biobank #1', 'This is the second test collection of biobank 1',
        'biobank:1:collection:2', 1, 4, 1),
       (6, 'Test collection #1 of biobank #2', 'This is the first test collection of biobank 2',
        'biobank:2:collection:1', 1, 5, 1),
       (7, 'Test collection #1 of biobank #3', 'This is the first test collection of biobank 3',
        'biobank:3:collection:1', 1, 6, 1),
       (8, 'Test collection #2 of biobank #3', 'This is the second test collection of biobank 3',
        'biobank:3:collection:2', 1, 6, 1),
       (9, 'Test collection #3 of biobank #3', 'This is the third test collection of biobank 3',
        'biobank:3:collection:3', 1, 6, 1);

-- TheBiobanker (109) represents resources of biobank1 and biobank2.
-- SareRepr (105) represents resources of biobank3
insert into RESOURCE_REPRESENTATIVE_LINK (resource_id, person_id)
values (4, 103),
       (4, 109),
       (5, 109),
       (6, 109),
       (7, 105),
       (8, 105),
       (9, 105);

insert into negotiation (id, creation_date, current_state, modified_date, created_by, modified_by, payload)
values ('negotiation-1', '2024-10-12', 'IN_PROGRESS', '2023-04-12', 108, 108,
        '{"project":{"title":"Biobanking project","description":"desc"},"samples":{"sample-type":"DNA","num-of-subjects": 10,"num-of-sample": "100","volume":3},"ethics-vote":{"ethics-vote":"My ethics"}}' FORMAT JSON),
       ('negotiation-2', '2024-03-12', 'SUBMITTED', '2024-04-12', 108, 108,
        '{"project":{"title":"Interesting project","description":"desc"},"samples":{"sample-type":"Plasma","num-of-subjects": 10,"num-of-sample": "100","volume":3},"ethics-vote":{"ethics-vote":"My ethics"}}' FORMAT JSON),
       ('negotiation-v2', '2023-04-12', 'ABANDONED', '2024-10-12', 108, 108,
        '{"project":{"title":"A Project 3","description":"Project 3 desc"},"samples":{"sample-type":"Blood","num-of-subjects": 5,"num-of-sample": "10","volume":4},"ethics-vote":{"ethics-vote":"My ethics"}}' FORMAT JSON),
       ('negotiation-3', '2024-02-24', 'IN_PROGRESS', '2024-02-24', 105, 105,
        '{"project":{"title":"Project 3","description":"Project 3 desc"},"samples":{"sample-type":"Blood","num-of-subjects": 5,"num-of-sample": "10","volume":4},"ethics-vote":{"ethics-vote":"My ethics"}}' FORMAT JSON),
       ('negotiation-4', '2024-01-10', 'ABANDONED', '2024-01-10', 105, 105,
        '{"project":{"title":"Project 3","description":"Project 3 desc"},"samples":{"sample-type":"Blood","num-of-subjects": 5,"num-of-sample": "10","volume":4},"ethics-vote":{"ethics-vote":"My ethics"}}' FORMAT JSON);

insert into request (id, url, human_readable, data_source_id, negotiation_id)
values ('request-1', 'http://datasource.dev', '#1: No filters used', 1, 'negotiation-1'),
       ('request-2', 'http://datasource.dev', '#1: DNA Samples', 1, null),
       ('request-v2', 'http://datasource.dev', '#1: Blood Samples', 1, 'negotiation-v2'),
       ('request-3', 'http://datasource.dev', '#1: Blood Samples', 1, 'negotiation-3'),
       ('request-4', 'http://datasource.dev', '#1: Blood Samples', 1, 'negotiation-4'),
       ('request-unassigned', 'http://datasource.dev', '#1: Blood Samples', 1, null);

insert into request_resources_link (request_id, resource_id)
values ('request-1', 4),
       ('request-2', 5),
       ('request-2', 6),
       ('request-v2', 7),
       ('request-3', 5),
       ('request-4', 5),
       ('request-unassigned', 7);

insert into resource_state_per_negotiation (negotiation_id, resource_id, current_state)
values ('negotiation-1', 'biobank:1:collection:1', 'SUBMITTED'),
       ('negotiation-1', 'biobank:1:collection:2', 'SUBMITTED'),
       ('negotiation-v2', 'biobank:3:collection:1', 'SUBMITTED');

-- Adding just representatives since the AUTHOR is the createdBy field of the negotiation
insert into person_negotiation_role (negotiation_id, person_id, role_id)
values ('negotiation-1', 109, 5);

insert into post (id, creation_date, modified_date, status, text, created_by, modified_by, negotiation_id, organization_id,
                  type)
values ('post-1-researcher', '2023-06-19', '2023-06-19', 'CREATED', 'post-1-researcher-message', 108, 108,
        'negotiation-1', null, 'PUBLIC'),
       ('post-2-researcher', '2023-06-19', '2023-06-19', 'CREATED', 'post-2-researcher-message', 108, 108,
        'negotiation-1', null, 'PUBLIC'),
       ('post-3-researcher', '2023-06-19', '2023-06-19', 'CREATED', 'post-2-researcher-message', 108, 108,
        'negotiation-1', 4, 'PRIVATE'),
       ('post-1-representative', '2023-06-19', '2023-06-19', 'CREATED', 'post-1-representative-message', 109, 109,
        'negotiation-1', null, 'PUBLIC'),
       ('post-2-representative', '2023-06-19', '2023-06-19', 'CREATED', 'post-2-representative-message', 109, 109,
        'negotiation-1', null, 'PUBLIC'),
       ('post-3-representative', '2023-06-19', '2023-06-19', 'CREATED', 'post-2-representative-message', 109, 109,
        'negotiation-1', 4, 'PRIVATE'),
       ('post-4-representative', '2023-06-19', '2023-06-19', 'CREATED', 'post-2-representative-message', 109, 109,
        'negotiation-1', 5, 'PRIVATE');
