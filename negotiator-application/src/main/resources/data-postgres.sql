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

insert into person
  (id, auth_email, auth_name, auth_subject, organization, person_image) values
  (1, 'researcher@negotiator.it', 'researcher', 'researcher', null, null);

insert into data_source
  (id, url, api_username, api_password, api_type, api_url, description, name,
   resource_biobank, resource_collection, resource_network, source_prefix, sync_active) values
  (1, 'http://datasource.dev', 'user', 'password', 'MOLGENIS', 'http://datasource.dev', 'Biobank Directory',
      'Biobank Directory', 'directory_biobanks', 'directory_collections', 'directory_networks', 'false', 'false');

insert into biobank (id, name, description, source_id)
values(1, 'Biobank for Test', 'This is a testing biobank', 'biobank:1');

insert into collection (id, name, description, source_id, data_source_id, biobank_id)
values(1, 'Collection for Test', 'This is a testing collection', 'collection:1', 1, 1);

--insert into person_biobank_link (biobank_id, person_id) values (1, 2);

--insert into person_collection_link (collection_id, person_id) values (1, 2);