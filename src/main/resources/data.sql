insert into role (id, name)
values (4, 'ROLE_RESEARCHER');

insert into data_source (url, api_username, api_password, api_type, api_url, description, name,
                         resource_biobank, resource_collection, resource_network, source_prefix, sync_active)
values ('https://bbmritestnn.gcc.rug.nl', 'user', 'password', 'MOLGENIS', 'https://bbmritestnn.gcc.rug.nl',
        'Biobank Directory', 'Biobank Directory', 'directory_biobanks', 'directory_collections',
        'directory_networks', 'source_prefix', 'false');

insert into person (id, auth_email, auth_name, auth_subject, password, organization, admin)
values (1, 'adam.researcher@gmail.com', 'TheResearcher', '1000@bbmri.eu', null, 'BBMRI', false),
       (2, 'taylor.biobanker@gmail.com', 'TheBiobanker', '1001@bbmri.eu', null, 'BBMRI', false),
       (3, 'for-backwards-compatability', 'directory', '',
        '$2y$10$6WCNhO3ppwgEN..vRrlQzuGv9Cng/eNIHjJx1vD2m8JfRWr6eMdFO', null, false);

insert into access_criteria_set (id, name)
values (1, 'BBMRI Template');

insert into access_criteria_section (id, name, label, description, access_criteria_set_id)
values (1, 'project', 'Project', 'Provide information about your project', 1);
insert into access_criteria_section (id, name, label, description, access_criteria_set_id)
values (2, 'request', 'Request', 'Provide information the resources you are requesting', 1);
insert into access_criteria_section (id, name, label, description, access_criteria_set_id)
values (3, 'ethics-vote', 'Ethics vote', 'Is ethics vote present in your project?', 1);
--insert into access_criteria_section (id, name, label, description, access_criteria_set_id)
--values (4, 'attachments', 'Attachments', 'Upload attachments:', 1);

insert into access_criteria (id, name, label, description, type)
values (1, 'title', 'Title', 'Give a title', 'text');
insert into access_criteria (id, name, label, description, type)
values (2, 'description', 'Description', 'Give a description', 'textarea');
insert into access_criteria (id, name, label, description, type)
values (3, 'ethics-vote', 'Ethics vote', 'Write the etchics vote', 'textarea');
insert into access_criteria (id, name, label, description, type)
values (4, 'ethics-vote-attachment', 'Attachment', 'Upload Ethics Vote', 'file');

insert into access_criteria_section_link (access_criteria_section_id, access_criteria_id, ordering, required)
values (1, 1, 1, 'true'),
       (1, 2, 2, 'false'),
       (2, 2, 1, 'true'),
       (3, 3, 1, 'false'),
       (3, 4, 2, 'false');

insert into organization (ID, external_id, name)
values (1, 'bbmri-eric:ID:SE_890', 'Biobank Väst'),
       (2, 'bbmri-eric:ID:CZ_MMCI',
        'Masaryk Memorial Cancer Institute');

insert into resource (id, name, description, source_id, data_source_id, access_criteria_set_id, organization_id)
values (1, 'Dummy test collection', 'This is the fist test collection',
        'bbmri-eric:ID:SE_890:collection:dummy_collection', 1, 1, 1),
       (2, 'Test collection 2', 'This is the second test collection',
        'bbmri-eric:ID:CZ_MMCI:collection:LTS', 1, 1, 2);
insert into RESOURCE_REPRESENTATIVE_LINK (RESOURCE_ID, PERSON_ID)
VALUES (2, 2)