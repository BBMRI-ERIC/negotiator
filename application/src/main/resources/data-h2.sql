insert into person
  (id, type, auth_email, auth_name, auth_subject, is_admin, organization, person_image)
values(1, 'INTERNAL', 'devadmin@negotiator.it', 'admin', 'admin', 'true', null, null);

insert into person
  (id, type, auth_email, auth_name, auth_subject, is_admin, organization, person_image)
values(2, 'EXTERNAL', 'researcher@negotiator.it', 'researcher', 'researcher', 'false', null, null);

insert into data_source
  (id, url, api_username, api_password, api_type, api_url, description, name, resource_biobank, resource_collection, resource_network, source_prefix, sync_active)
values (1, 'http://datasource.dev',
      'user', 'password', 'MOLGENIS', 'http://datasource.dev', 'Biobank Directory',
      'Biobank Directory', 'directory_biobanks', 'directory_collections', 'directory_networks', 'false', 'false');

insert into biobank (id, name, description, source_id)
values(1, 'Biobank for Test', 'This is a testing biobank', 'biobank:1');

insert into collection (id, name, description, source_id, data_source_id, biobank_id)
values(1, 'Collection for Test', 'This is a testing collection', 'collection:1', 1, 1);

--insert into person_biobank_link (biobank_id, person_id) values (1, 2);

--insert into person_collection_link (collection_id, person_id) values (1, 2);