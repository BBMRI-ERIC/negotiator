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
