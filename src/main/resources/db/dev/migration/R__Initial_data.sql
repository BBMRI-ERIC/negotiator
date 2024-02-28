insert into role (id, name)
values (1, 'CREATOR'),
       (2, 'ADMINISTRATOR'),
       (3, 'MANAGER'),
       (4, 'ROLE_RESEARCHER'),
       (5, 'REPRESENTATIVE');

insert into data_source (url, api_username, api_password, api_type, api_url, description, name,
                         resource_biobank, resource_collection, resource_network, source_prefix, sync_active)
values ('https://bbmritestnn.gcc.rug.nl', 'user', 'password', 'MOLGENIS', 'https://bbmritestnn.gcc.rug.nl',
        'Biobank Directory', 'Biobank Directory', 'directory_biobanks', 'directory_collections',
        'directory_networks', 'source_prefix', 'false');

insert into person (id, email, name, subject_id, password, organization, admin)
values (1, 'adam.researcher@gmail.com', 'TheResearcher', '1000@bbmri.eu', null, 'BBMRI', false),
       (2, 'taylor.biobanker@gmail.com', 'TheBiobanker', '1001@bbmri.eu', null, 'BBMRI', false),
       (3, 'for-backwards-compatability', 'directory', '',
        '$2y$10$6WCNhO3ppwgEN..vRrlQzuGv9Cng/eNIHjJx1vD2m8JfRWr6eMdFO', null, false);

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

insert into organization (ID, external_id, name)
values (1, 'bbmri-eric:ID:SE_890', 'Biobank Väst'),
       (2, 'bbmri-eric:ID:CZ_MMCI',
        'Masaryk Memorial Cancer Institute');

insert into resource (id, name, description, source_id, data_source_id, access_form_id, organization_id)
values (1, 'Dummy test collection', 'This is the fist test collection',
        'bbmri-eric:ID:SE_890:collection:dummy_collection', 1, 1, 1),
       (2, 'Test collection 2', 'This is the second test collection',
        'bbmri-eric:ID:CZ_MMCI:collection:LTS', 1, 1, 2);

insert into resource_representative_link (resource_id, person_id)
values (2, 2);