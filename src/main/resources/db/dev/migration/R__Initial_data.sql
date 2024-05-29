insert into role (id, name)
values (1, 'CREATOR'),
       (2, 'ADMINISTRATOR'),
       (3, 'MANAGER'),
       (4, 'ROLE_RESEARCHER'),
       (5, 'REPRESENTATIVE');

insert into discovery_service (url, name)
values ('https://bbmritestnn.gcc.rug.nl', 'Biobank Directory');

insert into person (id, email, name, subject_id, password, organization, admin)
values (1, 'adam.researcher@gmail.com', 'TheResearcher', '1000@bbmri.eu', null, 'BBMRI', false),
       (2, 'taylor.biobanker@gmail.com', 'TheBiobanker', '1001@bbmri.eu', null, 'BBMRI', false),
       (3, 'for-backwards-compatability', 'directory', '',
        '$2y$10$6WCNhO3ppwgEN..vRrlQzuGv9Cng/eNIHjJx1vD2m8JfRWr6eMdFO', null, false);

insert into organization (ID, external_id, name)
values (1, 'bbmri-eric:ID:SE_890', 'Biobank Väst'),
       (2, 'bbmri-eric:ID:CZ_MMCI',
        'Masaryk Memorial Cancer Institute');

insert into resource (id, name, description, source_id, discovery_service_id, access_form_id, organization_id)
values (1, 'Dummy test collection', 'This is the fist test collection',
        'bbmri-eric:ID:SE_890:collection:dummy_collection', 1, 1, 1),
       (2, 'Test collection 2', 'This is the second test collection',
        'bbmri-eric:ID:CZ_MMCI:collection:LTS', 1, 3, 2);

insert into resource_representative_link (resource_id, person_id)
values (2, 2);

insert into network (id, external_id, name, uri, contact_email)
values (1, 'bbmri-eric:ID:SE_890:network:bbmri-eric', 'network-1', 'https://network-1/', 'office@negotiator.org');

insert into network_resources_link (network_id, resource_id)
values (1, 1);

insert into network_person_link (network_id, person_id)
values (1, 1);

INSERT INTO public.value_set (id, name, external_documentation)
VALUES (100, 'multi_choice_test', 'none');
INSERT INTO public.value_set (id, name, external_documentation)
VALUES (101, 'single_choice_test', 'https://directory.bbmri-eric.eu/#/catalogue');

INSERT INTO public.value_set_available_values (value_set_id, available_values)
VALUES (100, 'first_choice');
INSERT INTO public.value_set_available_values (value_set_id, available_values)
VALUES (100, 'second_choice');
INSERT INTO public.value_set_available_values (value_set_id, available_values)
VALUES (100, 'third_choice');
INSERT INTO public.value_set_available_values (value_set_id, available_values)
VALUES (101, 'first_choice');
INSERT INTO public.value_set_available_values (value_set_id, available_values)
VALUES (101, 'second_choice');
INSERT INTO public.value_set_available_values (value_set_id, available_values)
VALUES (101, 'third_choice');
insert into public.access_form_element (id, creation_date, modified_date, created_by, modified_by, name, label,
                                        description, type, access_form_section_id, value_set_id)
values (100, '2024-05-29 13:14:33.787183', '2024-05-29 13:14:33.787183', 2, 2, 'Multichoice test', 'Select multiple',
        'Multiple selection', 'MULTIPLE_CHOICE', null, 100),
       (101, null, '2024-05-29 13:17:20.905824', 2, 2, 'Single test', 'Select one', 'Single selection', 'SINGLE_CHOICE',
        null, 101),
       (102, '2024-05-29 13:19:58.992884', '2024-05-29 13:19:58.992884', 2, 2, 'Bool test', 'Select one',
        'Yes-no selection', 'BOOLEAN', null, null);
insert into public.access_form_section_element_link (id, access_form_section_link_id, access_form_element_id,
                                                     is_required, element_order)
values (100, 7, 100, true, 10),
       (101, 7, 101, true, 11),
       (102, 7, 102, true, 12);