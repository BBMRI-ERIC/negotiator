insert into person
  (id, type, auth_email, auth_name, auth_subject, is_admin, organization, person_image)
values(1, 'LOCAL', 'admin@negotiator', 'admin', 'admin', 'true', null, null);

insert into data_source
  (id, url, api_username, api_password, api_type, api_url, description, "name", resource_biobank,
   resource_collection, resource_network, source_prefix, sync_active)
values (1, 'http://negotiator.local/', 'user', 'password', 'MOLGENIS', 'http://negotiator.local/',
        'Biobank Directory', 'Biobank Directory', 'directory_biobanks', 'directory_collections',
        'directory_networks', 'false', 'false');


insert into biobank (id, name, description, source_id, data_source_id)
values(1, 'Biobank for Test', 'This is a testing biobank', 'biobank:1', 1);

insert into collection (id, name, description, source_id, data_source_id)
values(1, 'Collection for Test', 'This is a testing collection', 'collection1', 1);