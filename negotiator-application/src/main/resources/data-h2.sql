insert into data_source (id, url, api_username, api_password, api_type, api_url, description, name,
   resource_biobank, resource_collection, resource_network, source_prefix, sync_active)
values
  (1, 'http://datasource.dev', 'user', 'password', 'MOLGENIS', 'http://datasource.dev',
   'Biobank Directory', 'Biobank Directory', 'directory_biobanks', 'directory_collections',
   'directory_networks', 'false', 'false');

insert into users (username, password, enabled) values
  ('admin', '$2a$10$Kk29y.f7WeQeyym0X7YnvewDm3Gm/puTWGFniJvWen93C/f/6Bqey', 'true'),
  ('directory', '$2a$10$iHi5bQ8nTRRF1bkiJfygkONgmABH1xNpLy2MZrHdusP.7.Rjpwk.i', 'true'),
  ('perun', '$2a$10$RCBPPd3suXNB4vLSowDdUe5umkyZaDJCt.8DtG3xVidUhxWe2Woci', 'true'),
  ('researcher', '$2a$10$6Rc4eC5vo2IMGP0KUgrxIObq2SQoHTBKx8/o/Eyq1PpmzdBtTKj0u', 'true');

insert into authorities (username, authority) values
  ('admin', 'ADMIN'),
  ('directory', 'EXT_SERV'),
  ('perun', 'PERUN_USER'),
  ('researcher', 'RESEARCHER');

insert into person (id, auth_email, auth_name, auth_subject, organization, person_image) values
  (1, 'researcher@negotiator.dev', 'researcher', 'researcher', 'Test Lab', null),
  (2, 'manager@testbiobank.dev', 'test_biobank_manager', 'test_biobank_manager', 'Test Biobank', null),
  (3, 'manager@testcollection.dev', 'test_collection_manager', 'test_collection_manager', 'Test Collection', null),
  (4, 'manager@testnetwork.dev', 'test_network_manager', 'test_network_manager', 'Test Network', null);

insert into biobank (id, name, description, source_id) values
  (1, 'Test biobank #1', 'This is the first testing biobank', 'biobank:1'),
  (2, 'Test biobank #2', 'This is the second testing biobank', 'biobank:2'),
  (3, 'Test biobank #3', 'This is the third testing biobank', 'biobank:3');

insert into collection (id, name, description, source_id, data_source_id, biobank_id) values
  (1, 'Test collection #1 of biobank #1', 'This is the first test collection of biobank 1', 'biobank:1:collection:1', 1, 1),
  (2, 'Test collection #2 of biobank #1', 'This is the second test collection of biobank 1', 'biobank:1:collection:2', 1, 1),
  (3, 'Test collection #1 of biobank #2', 'This is the first test collection of biobank 2', 'biobank:2:collection:1', 1, 2),
  (4, 'Test collection #1 of biobank #3', 'This is the first test collection of biobank 3', 'biobank:3:collection:1', 1, 3),
  (5, 'Test collection #2 of biobank #3', 'This is the second test collection of biobank 3', 'biobank:3:collection:2', 1, 3),
  (6, 'Test collection #3 of biobank #3', 'This is the third test collection of biobank 3', 'biobank:3:collection:3', 1, 3);


insert into network (id, name, description, source_id, acronym, data_source_id) values
  (1, 'Network for Test', 'This is a testing network', 'network:1', 'NFT', 1);

insert into person_biobank_link (biobank_id, person_id) values (1, 2);

insert into person_collection_link (collection_id, person_id) values (1, 3);

insert into person_network_link (network_id, person_id) values (1, 4);