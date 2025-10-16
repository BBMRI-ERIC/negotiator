insert into discovery_service (url, name)
values ('http://discoveryservice.dev','Biobank Directory');

insert into person (id, email, name, subject_id, password, organization, admin, last_login)
values (101, 'admin@negotiator.dev', 'admin', '1', '$2a$10$Kk29y.f7WeQeyym0X7YnvewDm3Gm/puTWGFniJvWen93C/f/6Bqey',
        'BBMRI', true, '2025-07-30'),
       (102, 'directory@negotiator.dev', 'directory', '2',
        '$2a$10$iHi5bQ8nTRRF1bkiJfygkONgmABH1xNpLy2MZrHdusP.7.Rjpwk.i', 'BBMRI', false, '2025-01-01'),
       (103, 'perun@negotiator.dev', 'perun', '3', '$2a$10$RCBPPd3suXNB4vLSowDdUe5umkyZaDJCt.8DtG3xVidUhxWe2Woci',
        'BBMRI', false, null),
       (104, 'researcher@negotiator.dev', 'researcher', '4',
        '$2a$10$6Rc4eC5vo2IMGP0KUgrxIObq2SQoHTBKx8/o/Eyq1PpmzdBtTKj0u', 'BBMRI', false, '2023-02-15'),
       (105, 'sarah.representative@gmail.com', 'SarahRepr', '5', null, 'Test Biobank', false, '2025-03-30'),
       (108, 'adam.researcher@gmail.com', 'TheResearcher', '1000@bbmri.eu', null, 'BBMRI', false, '2025-04-20'),
       (109, 'taylor.biobanker@gmail.com', 'TheBiobanker', '1001@bbmri.eu', null, 'BBMRI', false, '2025-02-10');

insert into authorities (person_id, authority)
values (101, 'ADMIN'),  -- still needed for data sources tests
       (101, 'ROLE_ADMIN'),
       (108, 'RESEARCHER'),  -- TheResearcher has only RESEARCHER authority
       (105, 'RESEARCHER'),  -- SarahRepr has both RESEARCHER and REPRESENTATIVE authority
       (105, 'ROLE_REPRESENTATIVE_biobank:3:collection:1'),
       (105, 'ROLE_REPRESENTATIVE_biobank:3:collection:2'),
       (105, 'ROLE_REPRESENTATIVE_biobank:3:collection:3'),
       (109, 'ROLE_REPRESENTATIVE_biobank:1:collection:1'),  -- TheBiobanker has only REPRESENTATIVE authority
       (109, 'ROLE_REPRESENTATIVE_biobank:1:collection:2'),
       (109, 'ROLE_REPRESENTATIVE_biobank:2:collection:1');

insert into organization (id, name, external_id, contact_email, description, uri, withdrawn)
values (4, 'Biobank #1', 'biobank:1', 'biobank1@test.org', 'Biobank #1', 'https://biobank1.org', false),
       (5, 'Biobank #2', 'biobank:2', 'biobank2@test.org', 'Biobank #2', 'https://biobank2.org', false),
       (6, 'Biobank #3', 'biobank:3', 'biobank3@test.org', 'Biobank #3', 'https://biobank3.org', false);

insert into resource (id, name, description, source_id, discovery_service_id, organization_id, access_form_id, contact_email, uri)
values (4, 'Test collection #1 of biobank #1', 'This is the first test collection of biobank 1',
        'biobank:1:collection:1', 1, 4, 1, 'coll1bb1@test.org', 'https://biobank1.org/collection1'),
       (5, 'Test collection #2 of biobank #1', 'This is the second test collection of biobank 1',
        'biobank:1:collection:2', 1, 4, 1, 'coll2bb1@test.org', 'https://biobank1.org/collection2'),
       (6, 'Test collection #1 of biobank #2', 'This is the first test collection of biobank 2',
        'biobank:2:collection:1', 1, 5, 1, 'coll1bb2@test.org', 'https://biobank2.org/collection1'),
       (7, 'Test collection #1 of biobank #3', 'This is the first test collection of biobank 3',
        'biobank:3:collection:1', 1, 6, 1,'coll1bb3@test.org', 'https://biobank3.org/collection1'),
       (8, 'Test collection #2 of biobank #3', 'This is the second test collection of biobank 3',
        'biobank:3:collection:2', 1, 6, 1, 'coll2bb3@test.org', 'https://biobank3.org/collection2'),
       (9, 'Test collection #3 of biobank #3', 'This is the third test collection of biobank 3',
        'biobank:3:collection:3', 1, 6, 1, 'coll3bb3@test.org', 'https://biobank3.org/collection3'),
       (10, 'Test collection #10 of biobank #3', 'This is the third test collection of biobank 3',
        'biobank:3:collection:4', 1, 6, 1,'coll10bb3@test.org', 'https://biobank3.org/collection10');

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

insert into negotiation (id, creation_date, current_state, modified_date, created_by, modified_by, human_readable, payload, private_posts_enabled, public_posts_enabled, discovery_service_id)
values ('negotiation-1', '2024-10-12', 'IN_PROGRESS', '2024-10-12', 108, 108, '#1 Material Type: DNA',
        '{"project":{"title":"Biobanking project","description":"desc"},"samples":{"sample-type":"DNA","num-of-subjects": 10,"num-of-sample": "100","volume":3},"ethics-vote":{"ethics-vote":"My ethics"}}',
        true, true, 1),
       ('negotiation-2', '2024-03-12', 'SUBMITTED', '2024-04-02', 108, 108, '#1 Material Type: DNA; #2 Diagnosis: C18.2',
        '{"project":{"title":"Interesting project","description":"desc"},"samples":{"sample-type":"Plasma","num-of-subjects": 10,"num-of-sample": "100","volume":3},"ethics-vote":{"ethics-vote":"My ethics"}}',
        false, true, 1),
       ('negotiation-v2', '2023-04-12', 'ABANDONED', '2024-10-12', 108, 108, '#1 Diagnosis: C18.2',
        '{"project":{"title":"A Project 3","description":"Project 3 desc"},"samples":{"sample-type":"Blood","num-of-subjects": 5,"num-of-sample": "10","volume":4},"ethics-vote":{"ethics-vote":"My ethics"}}',
        false, false, 1),
       ('negotiation-3', '2024-02-24', 'IN_PROGRESS', '2024-02-24', 105, 105, '#1 Type: RD',
        '{"project":{"title":"Project 3","description":"Project 3 desc"},"samples":{"sample-type":"Blood","num-of-subjects": 5,"num-of-sample": "10","volume":4},"ethics-vote":{"ethics-vote":"My ethics"}}',
        true, true, 1),
       ('negotiation-4', '2024-01-10', 'ABANDONED', '2024-01-10', 105, 105, '#1 Type: Cohort',
        '{"project":{"title":"Project 4","description":"Project 4 desc"},"samples":{"sample-type":"Blood","num-of-subjects": 5,"num-of-sample": "10","volume":4},"ethics-vote":{"ethics-vote":"My ethics"}}',
        false, false, 1),
       ('negotiation-5', '2024-03-11', 'SUBMITTED', '2024-04-12', 108, 108, '#1 Quality: ISO',
        '{"project":{"title":"Yet another important project","description":"desc with special characters: !§$%&/()=?<>|¹²³¼½¬{[]} \n","checklist":["one","two"]},"samples":{"sample-type":"Plasma","num-of-subjects": 10,"num-of-sample": "100","volume":3},"ethics-vote":{"ethics-vote":"My ethics","ethics-vote-attachment":{}}}',
        false, true, 1);

insert into negotiation_resource_lifecycle_record (created_by, creation_date, modified_by, modified_date, changed_to, negotiation_id, resource_id)
values (101, '2024-03-11', 101, '2024-03-31', 'REPRESENTATIVE_CONTACTED', 'negotiation-1', 4),
       (101, '2024-03-11', 101, '2024-03-31', 'REPRESENTATIVE_CONTACTED', 'negotiation-3', 5),
       (101, '2024-03-11', 101, '2024-03-31', 'RESOURCE_AVAILABLE', 'negotiation-3', 5);
insert into negotiation_lifecycle_record (changed_to, creation_date, negotiation_id, modified_date, created_by, modified_by) VALUES ('IN_PROGRESS', '2023-06-19 10:15:00', 'negotiation-1', '2023-06-19 10:15:00', 101, 101);
insert into request (id, url, human_readable, discovery_service_id)
values ('request-1', 'http://discoveryservice.dev', '#1: No filters used', 1),
       ('request-2', 'http://discoveryservice.dev', '#1: DNA Samples', 1),
       ('request-5', 'http://discoveryservice.dev', '#1: DNA Samples', 1),
       ('request-v2', 'http://discoveryservice.dev', '#1: Blood Samples', 1),
       ('request-3', 'http://discoveryservice.dev', '#1: Blood Samples', 1),
       ('request-4', 'http://discoveryservice.dev', '#1: Blood Samples', 1),
       ('request-unassigned', 'http://discoveryservice.dev', '#1: Blood Samples', 1);

insert into request_resources_link (request_id, resource_id)
values ('request-1', 4),
       ('request-2', 5),
       ('request-2', 7),
       ('request-v2', 7),
       ('request-3', 5),
       ('request-4', 5),
       ('request-4', 7),
       ('request-5', 5),
       ('request-5', 7);

insert into negotiation_resource_link (negotiation_id, resource_id, current_state)
values ('negotiation-1', 4, 'SUBMITTED'),
       ('negotiation-v2', 7, 'SUBMITTED'),
       ('negotiation-3', 5, 'RESOURCE_UNAVAILABLE'),
       ('negotiation-4', 5, null),
       ('negotiation-4', 7, null),
       ('negotiation-5', 5, null),
       ('negotiation-5', 7, null);

insert into post (id, creation_date, modified_date, text, created_by, modified_by, negotiation_id, organization_id,
                  type)
values ('post-1-researcher', '2023-06-19 10:15:00', '2023-06-19 10:15:00', 'post-1-researcher-message', 108, 108,
        'negotiation-1', null, 'PUBLIC'),
       ('post-2-researcher', '2023-06-19 11:30:00', '2023-06-19 11:30:00', 'post-2-researcher-message', 108, 108,
        'negotiation-1', null, 'PUBLIC'),
       ('post-3-researcher', '2023-06-19 13:45:00', '2023-06-19 13:45:00', 'post-3-researcher-message', 108, 108,
        'negotiation-1', 4, 'PRIVATE'),
       ('post-1-representative', '2023-06-19 09:00:00', '2023-06-19 09:00:00', 'post-1-representative-message', 109,
        109, 'negotiation-1', null, 'PUBLIC'),
       ('post-2-representative', '2023-06-19 14:20:00', '2023-06-19 14:20:00', 'post-2-representative-message', 109,
        109, 'negotiation-1', null, 'PUBLIC'),
       ('post-3-representative', '2023-06-19 15:10:00', '2023-06-19 15:10:00', 'post-3-representative-message', 109,
        109, 'negotiation-1', 4, 'PRIVATE'),
       ('post-4-representative', '2023-06-19 16:05:00', '2023-06-19 16:05:00', 'post-4-representative-message', 109,
        109, 'negotiation-1', 5, 'PRIVATE');


insert into attachment (id, creation_date, modified_date, content_type, name, payload, size, created_by, modified_by,
                        negotiation_id, organization_id)
values ('attachment-1', '2024-04-12', '2024-04-12', 'application/pdf', 'Attachment #1', 'AB'::bytea, 16, 108, 108,
         'negotiation-5', null),  -- sent by creator publicly
       ('attachment-2', '2024-04-12', '2024-04-12', 'application/pdf', 'Attachment #2', 'CD'::bytea, 16, 108, 108,
         'negotiation-5', 4), -- sent by creator to biobank:1
       ('attachment-3', '2024-04-12', '2024-04-12', 'application/pdf', 'Attachment #1', 'AB'::bytea, 16, 108, 108,
         'negotiation-5', 6),  -- sent by creator to biobank:3
       ('attachment-4', '2024-04-12', '2024-04-12', 'application/pdf', 'Attachment #3', 'EF'::bytea, 16, 109, 109,
         'negotiation-5', null),  -- sent by biobanker publicly
       ('attachment-5', '2024-04-12', '2024-04-12', 'application/pdf', 'Attachment #4', '12'::bytea, 16, 109, 109,
         'negotiation-5', 4);  -- sent by biobanker to his/her organization


insert into network (id, external_id, name, uri, contact_email, description)
values (1, 'bbmri-eric:ID:SE_890:network:bbmri-eric', 'network-1', 'https://network-1/', 'office@negotiator.org', 'Network 1'),
       (2, 'bbmri-eric:ID:SE_891:network:bbmri-eric', 'network-2', 'https://network-2/', 'office@negotiator.org', 'Network 2'),
       (3, 'bbmri-eric:ID:SE_892:network:bbmri-eric', 'network-3', 'https://network-3/', 'office@negotiator.org', 'Network 3');

insert into network_resources_link (network_id, resource_id)
values (1, 4),
       (1, 5),
       (1, 6);

insert into network_person_link (network_id, person_id)
values (1, 101),
       (1, 102);

INSERT INTO ui_parameter (id,category,"name","type",value) VALUES (1001, 'theme', 'activeThemeFile', 'STRING',
                                                                   'default'),
                                                                  (1002, 'theme', 'activeLogosFiles', 'STRING',
                                                                   'default'),
                                                                  (1004, 'footer', 'footerHeight', 'INT', '50');
