insert into role (id, name) values
  (1, 'CREATOR'),
  (2, 'ADMINISTRATOR'),
  (3, 'MANAGER'),
  (4, 'RESEARCHER');

insert into data_source (url, api_username, api_password, api_type, api_url, description, name,
   resource_biobank, resource_collection, resource_network, source_prefix, sync_active)
values
  ('http://datasource.dev', 'user', 'password', 'MOLGENIS', 'http://datasource.dev',
   'Biobank Directory', 'Biobank Directory', 'directory_biobanks', 'directory_collections',
   'directory_networks', 'source_prefix', 'false');

insert into person (id, auth_email, auth_name, auth_subject, password, organization, person_image) values
  (101, 'admin@negotiator.dev', 'admin', '1', '$2a$10$Kk29y.f7WeQeyym0X7YnvewDm3Gm/puTWGFniJvWen93C/f/6Bqey', 'BBMRI', null),
  (102, 'directory@negotiator.dev', 'directory', '2', '$2a$10$iHi5bQ8nTRRF1bkiJfygkONgmABH1xNpLy2MZrHdusP.7.Rjpwk.i', 'BBMRI', null),
  (103, 'perun@negotiator.dev', 'perun', '3', '$2a$10$RCBPPd3suXNB4vLSowDdUe5umkyZaDJCt.8DtG3xVidUhxWe2Woci', 'BBMRI', null),
  (104, 'researcher@negotiator.dev', 'researcher', '4', '$2a$10$6Rc4eC5vo2IMGP0KUgrxIObq2SQoHTBKx8/o/Eyq1PpmzdBtTKj0u', 'BBMRI', null),
  (105, 'manager@testbiobank.dev', 'test_biobank_manager', '5', null, 'Test Biobank', null),
  (106, 'manager@testcollection.dev', 'test_collection_manager', '6', null, 'Test Collection', null),
  (107, 'manager@testnetwork.dev', 'test_network_manager', '7', null, 'Test Network', null),
  (108, 'adam.researcher@gmail.com', 'TheResearcher', '1000@bbmri.eu', null, 'BBMRI', null),
  (109, 'taylor.biobanker@gmail.com', 'TheBiobanker', '1001@bbmri.eu', null, 'BBMRI', null);

insert into authorities (person_id, authority) values
  (101, 'ADMIN'),
  (102, 'EXT_SERV'),
  (103, 'PERUN_USER'),
  (104, 'RESEARCHER'),
  (108, 'RESEARCHER');

insert into access_criteria_set (id, name) values (1, 'BBMRI Template');

insert into access_criteria_section (id, name, label, description, access_criteria_set_id) values (1, 'project', 'Project', 'Provide information about your project', 1);
insert into access_criteria_section (id, name, label, description, access_criteria_set_id) values (2, 'samples', 'Biosamples and Data Information', 'Provide information about the biosamples you want', 1);
insert into access_criteria_section (id, name, label, description, access_criteria_set_id) values (3, 'ethics-vote', 'Ethics vote', 'Is ethics vote present in your project?', 1);

insert into access_criteria (id, name, label, description, type) values (1, 'title', 'Title', 'Give a title', 'text');
insert into access_criteria (id, name, label, description, type) values (2, 'description', 'Description', 'Give a description', 'textarea');
insert into access_criteria (id, name, label, description, type) values (3, 'num-of-subjects', 'Number of subjects', 'Number of biosamples', 'number');
insert into access_criteria (id, name, label, description, type) values (4, 'sample-type', 'Sample type(s)', 'Sample Type', 'text');
insert into access_criteria (id, name, label, description, type) values (5, 'num-of-sample', 'Number of sample', 'Sample Type', 'text');
insert into access_criteria (id, name, label, description, type) values (6, 'volume', 'Volume', 'Write the etchics vote', 'number');
insert into access_criteria (id, name, label, description, type) values (7, 'ethics-vote', 'Ethics vote', 'Write the etchics vote', 'text');

insert into access_criteria_section_link (access_criteria_section_id, access_criteria_id, ordering, required) values
  (1, 1, 1, 'true'),
  (1, 2, 2, 'false'),
  (2, 3, 1, 'true'),
  (2, 4, 2, 'false'),
  (2, 5, 3, 'true'),
  (2, 6, 4, 'false'),
  (3, 7, 1, 'true');

insert into resource (id, name, description, source_id, type, parent_id, data_source_id, access_criteria_set_id) values
  (1, 'Test biobank #1', 'This is the first testing biobank', 'biobank:1', 'biobank' ,null, 1, 1),
  (2, 'Test biobank #2', 'This is the second testing biobank', 'biobank:2', 'biobank', null, 1, 1),
  (3, 'Test biobank #3', 'This is the third testing biobank', 'biobank:3', 'biobank', null, 1, 1),
  (4, 'Test collection #1 of biobank #1', 'This is the first test collection of biobank 1', 'biobank:1:collection:1', 'collection', 1, 1, 1),
  (5, 'Test collection #2 of biobank #1', 'This is the second test collection of biobank 1', 'biobank:1:collection:2', 'collection', 1, 1, 1),
  (6, 'Test collection #1 of biobank #2', 'This is the first test collection of biobank 2', 'biobank:2:collection:1', 'collection', 2, 1, 1),
  (7, 'Test collection #1 of biobank #3', 'This is the first test collection of biobank 3', 'biobank:3:collection:1', 'collection', 3, 1, 1),
  (8, 'Test collection #2 of biobank #3', 'This is the second test collection of biobank 3', 'biobank:3:collection:2', 'collection', 3, 1, 1),
  (9, 'Test collection #3 of biobank #3', 'This is the third test collection of biobank 3', 'biobank:3:collection:3', 'collection', 3, 1, 1);

insert into person_resource_link (resource_id, person_id) values (1, 102), (4, 103), (4, 109);

insert into negotiation (id, creation_date, modified_date, created_by, modified_by, payload) values
  ('negotiation-1', '2023-04-12', '2023-04-12', 108, 108, '{"project":{"title":"title","description":"desc"},"samples":{"sample-type":"DNA","num-of-subjects": 10,"num-of-sample": "100","volume":3},"ethics-vote":{"ethics-vote":"My ethics"}}' FORMAT JSON),
  ('negotiation-2', '2023-04-12', '2023-04-12', 108, 108, '{"project":{"title":"title","description":"desc"},"samples":{"sample-type":"Plasma","num-of-subjects": 10,"num-of-sample": "100","volume":3},"ethics-vote":{"ethics-vote":"My ethics"}}' FORMAT JSON),
  ('negotiation-v2', '2023-04-12', '2023-04-12', 108, 108, '{"project":{"title":"Project 3","description":"Project 3 desc"},"samples":{"sample-type":"Blood","num-of-subjects": 5,"num-of-sample": "10","volume":4},"ethics-vote":{"ethics-vote":"My ethics"}}' FORMAT JSON);

insert into request (id, url, human_readable, data_source_id, negotiation_id) values
  ('request-1', 'http://datasource.dev', '#1: No filters used', 1, 'negotiation-1'),
  ('request-2', 'http://datasource.dev', '#1: DNA Samples', 1, null),
  ('request-v2', 'http://datasource.dev', '#1: Blood Samples', 1, 'negotiation-v2'),
  ('request-unassigned', 'http://datasource.dev', '#1: Blood Samples', 1, null);

insert into request_resources_link (request_id, resource_id) values
  ('request-1', 4),
  ('request-2', 5),
  ('request-2', 6),
  ('request-v2', 7),
  ('request-unassigned', 7);

insert into person_negotiation (negotiation_id, person_id, role_id) values ('negotiation-1', 108, 4);
insert into person_negotiation (negotiation_id, person_id, role_id) values ('negotiation-2', 108, 4);
insert into person_negotiation (negotiation_id, person_id, role_id) values ('negotiation-v2', 108, 4);