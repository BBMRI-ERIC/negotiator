insert into role (id, name) values
  (1, 'CREATOR'),
  (2, 'ADMINISTRATOR'),
  (3, 'MANAGER'),
  (4, 'RESEARCHER');

insert into data_source (id, url, api_username, api_password, api_type, api_url, description, name,
   resource_biobank, resource_collection, resource_network, source_prefix, sync_active)
values
  (1, 'http://datasource.dev', 'user', 'password', 'MOLGENIS', 'http://datasource.dev',
   'Biobank Directory', 'Biobank Directory', 'directory_biobanks', 'directory_collections',
   'directory_networks', 'source_prefix', 'false');

insert into person (id, auth_email, auth_name, auth_subject, password, organization, person_image) values
  (1, 'admin@negotiator.dev', 'admin', '1', '$2a$10$Kk29y.f7WeQeyym0X7YnvewDm3Gm/puTWGFniJvWen93C/f/6Bqey', 'BBMRI', null),
  (2, 'directory@negotiator.dev', 'directory', '2', '$2a$10$iHi5bQ8nTRRF1bkiJfygkONgmABH1xNpLy2MZrHdusP.7.Rjpwk.i', 'BBMRI', null),
  (3, 'perun@negoatiator.dev', 'perun', '3', '$2a$10$RCBPPd3suXNB4vLSowDdUe5umkyZaDJCt.8DtG3xVidUhxWe2Woci', 'BBMRI', null),
  (4, 'researcher@negoatiator.dev', 'researcher', '4', '$2a$10$6Rc4eC5vo2IMGP0KUgrxIObq2SQoHTBKx8/o/Eyq1PpmzdBtTKj0u', 'BBMRI', null),
  (5, 'manager@testbiobank.dev', 'test_biobank_manager', '5', null, 'Test Biobank', null),
  (6, 'manager@testcollection.dev', 'test_collection_manager', '6', null, 'Test Collection', null),
  (7, 'manager@testnetwork.dev', 'test_network_manager', '7', null, 'Test Network', null);

insert into authorities (person_id, authority) values
  (1, 'ADMIN'),
  (2, 'EXT_SERV'),
  (3, 'PERUN_USER'),
  (4, 'RESEARCHER');

insert into resource (id, name, description, source_id, type, parent_id, data_source_id) values
  (1, 'Test biobank #1', 'This is the first testing biobank', 'biobank:1', 'biobank' ,null, 1),
  (2, 'Test biobank #2', 'This is the second testing biobank', 'biobank:2', 'biobank', null, 1),
  (3, 'Test biobank #3', 'This is the third testing biobank', 'biobank:3', 'biobank', null, 1),
  (4, 'Test collection #1 of biobank #1', 'This is the first test collection of biobank 1', 'biobank:1:collection:1', 'collection', 1, 1),
  (5, 'Test collection #2 of biobank #1', 'This is the second test collection of biobank 1', 'biobank:1:collection:2', 'collection', 1, 1),
  (6, 'Test collection #1 of biobank #2', 'This is the first test collection of biobank 2', 'biobank:2:collection:1', 'collection', 2, 1),
  (7, 'Test collection #1 of biobank #3', 'This is the first test collection of biobank 3', 'biobank:3:collection:1', 'collection', 3, 1),
  (8, 'Test collection #2 of biobank #3', 'This is the second test collection of biobank 3', 'biobank:3:collection:2', 'collection', 3, 1),
  (9, 'Test collection #3 of biobank #3', 'This is the third test collection of biobank 3', 'biobank:3:collection:3', 'collection', 3, 1);

insert into person_resource_link (resource_id, person_id) values (1, 2), (4, 3);
