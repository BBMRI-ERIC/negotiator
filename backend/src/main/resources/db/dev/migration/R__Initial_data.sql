insert into discovery_service (id, url, name)
values (1, 'https://bbmritestnn.gcc.rug.nl', 'Biobank Directory');

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

insert into public.value_set (id, name, external_documentation)
values (100, 'multi_choice_test', 'none');
insert into public.value_set (id, name, external_documentation)
values (101, 'single_choice_test', 'https://directory.bbmri-eric.eu/#/catalogue');

insert into public.value_set_available_values (value_set_id, available_values)
values (100, 'first_choice');
insert into public.value_set_available_values (value_set_id, available_values)
values (100, 'second_choice');
insert into public.value_set_available_values (value_set_id, available_values)
values (100, 'third_choice');
insert into public.value_set_available_values (value_set_id, available_values)
values (101, 'first_choice');
insert into public.value_set_available_values (value_set_id, available_values)
values (101, 'second_choice');
insert into public.value_set_available_values (value_set_id, available_values)
values (101, 'third_choice');
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

insert into negotiation (id, creation_date, modified_date, current_state, payload, public_posts_enabled, private_posts_enabled, created_by, modified_by, human_readable, discovery_service_id)
values ('550e8400-e29b-41d4-a716-446655440000', '2024-01-01 10:00:00', '2024-01-01 10:00:00', 'SUBMITTED', '{"project":{"title":"HIV Cohort Study","disease-code":"B23.0","objective":"To investigate disease progression and comorbidities in people living with HIV/AIDS","organization":"University Medical Center","profit":"No","acknowledgment":"Funded by National Institute of Health Research","Multichoice test":["second_choice","third_choice"],"Single test":"second_choice","Bool test":"Yes"},"request":{"description":"We seek access to HIV cohort samples with documented disease progression markers and comorbidity data","collection":"","donors":"150","samples":"serum","specifics":"CD4 count and viral load data required"},"ethics-vote":{"ethics-vote":"Approved by University Ethics Committee","ethics-vote-attachment":null}}', true, false, 1, 1, '(((gender EQUALS male) OR (gender EQUALS female)) AND (diagnosis EQUALS B23.0))', 1),
       ('550e8400-e29b-41d4-a716-446655440001', '2024-01-02 11:00:00', '2024-01-02 11:00:00', 'IN_PROGRESS', '{"project":{"title":"Type 2 Diabetes Research Initiative","disease-code":"E11.9","objective":"To identify biomarkers and genetic factors associated with Type 2 Diabetes risk in European populations","organization":"Diabetes Research Center","profit":"No","acknowledgment":"European Foundation for Diabetes Research","Multichoice test":["first_choice","second_choice"],"Single test":"first_choice","Bool test":"Yes"},"request":{"description":"Request de-identified Type 2 Diabetes patient samples with metabolic panel results and family history","collection":"","donors":"300","samples":"blood","specifics":"HbA1c and fasting glucose values required"},"ethics-vote":{"ethics-vote":"Approved by Institutional Review Board","ethics-vote-attachment":null}}', true, true, 1, 1, '((age GREATER_THAN_OR_EQUAL 18) AND (age LESS_THAN_OR_EQUAL 75) AND (diagnosis EQUALS E11.9))', 1),
        ('550e8400-e29b-41d4-a716-446655440002', '2024-01-03 12:00:00', '2024-01-03 12:00:00', 'CONCLUDED', '{"project":{"title":"Cardiovascular Risk Assessment Study","disease-code":"I10,I11.9,I48.9","objective":"To develop novel risk prediction models for cardiovascular disease in hypertensive and arrhythmic patients","organization":"Biobank Väst","profit":"No","acknowledgment":"Cardiovascular Research Foundation","Multichoice test":["first_choice","third_choice"],"Single test":"third_choice","Bool test":"Yes"},"request":{"description":"We require blood samples from hypertensive patients with documented cardiovascular events or atrial fibrillation","collection":"","donors":"200","samples":"blood","specifics":"Blood pressure recordings and ECG data available"},"ethics-vote":{"ethics-vote":"Approved by Ethics Board","ethics-vote-attachment":null}}', true, false, 1, 1, '(((diagnosis EQUALS I10) OR (diagnosis EQUALS I11.9) OR (diagnosis EQUALS I48.9)) AND (sample_type EQUALS blood))', 1),
        ('550e8400-e29b-41d4-a716-446655440003', '2024-01-04 09:00:00', '2024-01-04 09:00:00', 'SUBMITTED', '{"project":{"title":"Lung Cancer Genomics Study","disease-code":"C34.9,C34.1,C34.2","objective":"To characterize genomic alterations in non-small cell lung cancer to guide precision medicine approaches","organization":"Masaryk Memorial Cancer Institute","profit":"No","acknowledgment":"International Cancer Genomics Consortium","Multichoice test":["second_choice","third_choice"],"Single test":"second_choice","Bool test":"Yes"},"request":{"description":"Seeking tumor tissue and matched normal tissue samples from lung cancer patients for comprehensive genomic analysis","collection":"","donors":"100","samples":"tissue","specifics":"NSCLC subtypes preferred with pathology confirmation"},"ethics-vote":{"ethics-vote":"Approved by Cancer Research Ethics Committee","ethics-vote-attachment":null}}', true, false, 1, 1, '((diagnosis EQUALS C34.9) OR (diagnosis EQUALS C34.1) OR (diagnosis EQUALS C34.2))', 1);

insert into negotiation_lifecycle_record (creation_date, modified_date, created_by, modified_by, changed_to, negotiation_id)
values ('2024-01-01 08:00:00', '2024-01-01 08:00:00', 1, 1, 'DRAFT', '550e8400-e29b-41d4-a716-446655440000'),
       ('2024-01-01 09:30:00', '2024-01-01 09:30:00', 1, 1, 'SUBMITTED', '550e8400-e29b-41d4-a716-446655440000'),
       ('2024-01-02 08:00:00', '2024-01-02 08:00:00', 1, 1, 'DRAFT', '550e8400-e29b-41d4-a716-446655440001'),
       ('2024-01-02 10:00:00', '2024-01-02 10:00:00', 1, 1, 'SUBMITTED', '550e8400-e29b-41d4-a716-446655440001'),
       ('2024-01-02 11:00:00', '2024-01-02 11:00:00', 2, 2, 'IN_PROGRESS', '550e8400-e29b-41d4-a716-446655440001'),
       ('2024-01-03 08:00:00', '2024-01-03 08:00:00', 1, 1, 'DRAFT', '550e8400-e29b-41d4-a716-446655440002'),
       ('2024-01-03 11:00:00', '2024-01-03 11:00:00', 1, 1, 'SUBMITTED', '550e8400-e29b-41d4-a716-446655440002'),
       ('2024-01-03 12:15:00', '2024-01-03 12:15:00', 2, 2, 'IN_PROGRESS', '550e8400-e29b-41d4-a716-446655440002'),
       ('2024-01-03 13:30:00', '2024-01-03 13:30:00', 2, 2, 'APPROVED', '550e8400-e29b-41d4-a716-446655440002'),
       ('2024-01-03 14:00:00', '2024-01-03 14:00:00', 2, 2, 'CONCLUDED', '550e8400-e29b-41d4-a716-446655440002'),
       ('2024-01-04 09:00:00', '2024-01-04 09:00:00', 1, 1, 'DRAFT', '550e8400-e29b-41d4-a716-446655440003'),
       ('2024-01-04 09:01:00', '2024-01-04 09:01:00', 1, 1, 'SUBMITTED', '550e8400-e29b-41d4-a716-446655440003');

insert into negotiation_resource_link (negotiation_id, resource_id, current_state)
values ('550e8400-e29b-41d4-a716-446655440000', 1, 'SUBMITTED'),
       ('550e8400-e29b-41d4-a716-446655440001', 1, 'RESOURCE_AVAILABLE'),
       ('550e8400-e29b-41d4-a716-446655440002', 1, 'RESOURCE_MADE_AVAILABLE'),
       ('550e8400-e29b-41d4-a716-446655440003', 2, 'SUBMITTED');

insert into negotiation_resource_lifecycle_record (created_by, creation_date, modified_date, modified_by, changed_to, negotiation_id, resource_id)
values (1, '2024-01-01 10:00:00', '2024-01-01 10:00:00', 1, 'SUBMITTED', '550e8400-e29b-41d4-a716-446655440000', 1),
       (2, '2024-01-01 10:15:00', '2024-01-01 10:15:00', 2, 'REPRESENTATIVE_CONTACTED', '550e8400-e29b-41d4-a716-446655440000', 1),
       (1, '2024-01-02 10:05:00', '2024-01-02 10:05:00', 1, 'SUBMITTED', '550e8400-e29b-41d4-a716-446655440001', 1),
       (2, '2024-01-02 10:20:00', '2024-01-02 10:20:00', 2, 'REPRESENTATIVE_CONTACTED', '550e8400-e29b-41d4-a716-446655440001', 1),
       (2, '2024-01-02 10:50:00', '2024-01-02 10:50:00', 2, 'CHECKING_AVAILABILITY', '550e8400-e29b-41d4-a716-446655440001', 1),
       (2, '2024-01-02 11:00:00', '2024-01-02 11:00:00', 2, 'RESOURCE_AVAILABLE', '550e8400-e29b-41d4-a716-446655440001', 1),
       (1, '2024-01-03 11:05:00', '2024-01-03 11:05:00', 1, 'SUBMITTED', '550e8400-e29b-41d4-a716-446655440002', 1),
       (2, '2024-01-03 12:10:00', '2024-01-03 12:10:00', 2, 'REPRESENTATIVE_CONTACTED', '550e8400-e29b-41d4-a716-446655440002', 1),
       (2, '2024-01-03 12:30:00', '2024-01-03 12:30:00', 2, 'CHECKING_AVAILABILITY', '550e8400-e29b-41d4-a716-446655440002', 1),
       (2, '2024-01-03 13:30:00', '2024-01-03 13:30:00', 2, 'RESOURCE_AVAILABLE', '550e8400-e29b-41d4-a716-446655440002', 1),
       (2, '2024-01-03 14:05:00', '2024-01-03 14:05:00', 2, 'RESOURCE_MADE_AVAILABLE', '550e8400-e29b-41d4-a716-446655440002', 1),
       (2, '2024-01-04 11:00:00', '2024-01-04 11:00:00', 2, 'CHECKING_AVAILABILITY', '550e8400-e29b-41d4-a716-446655440003', 2);

insert into post (id, creation_date, modified_date, created_by, modified_by, type, text, negotiation_id)
values ('post-550e8400-e29b-41d4-a716-446655440000-001', '2024-01-01 09:45:00', '2024-01-01 09:45:00', 1, 1, 'PUBLIC', 'We are requesting access to HIV cohort samples for our research on disease progression. Please review our detailed access form.', '550e8400-e29b-41d4-a716-446655440000'),
       ('post-550e8400-e29b-41d4-a716-446655440000-002', '2024-01-01 10:20:00', '2024-01-01 10:20:00', 2, 2, 'PUBLIC', 'Thank you for your submission. We have received your request and will begin our internal assessment process.', '550e8400-e29b-41d4-a716-446655440000'),
       ('post-550e8400-e29b-41d4-a716-446655440001-001', '2024-01-02 10:10:00', '2024-01-02 10:10:00', 1, 1, 'PUBLIC', 'Submitting our diabetes research request. We need de-identified Type 2 diabetes patient samples aged 18-75 with complete metabolic data.', '550e8400-e29b-41d4-a716-446655440001'),
       ('post-550e8400-e29b-41d4-a716-446655440001-002', '2024-01-02 11:05:00', '2024-01-02 11:05:00', 2, 2, 'PUBLIC', 'Great news! We have approved your request and samples are being prepared for transfer.', '550e8400-e29b-41d4-a716-446655440001'),
       ('post-550e8400-e29b-41d4-a716-446655440002-001', '2024-01-03 11:10:00', '2024-01-03 11:10:00', 1, 1, 'PUBLIC', 'We are requesting blood samples from hypertension and cardiovascular patients for our risk assessment research.', '550e8400-e29b-41d4-a716-446655440002'),
       ('post-550e8400-e29b-41d4-a716-446655440002-002', '2024-01-03 12:20:00', '2024-01-03 12:20:00', 2, 2, 'PUBLIC', 'Excellent request. We have comprehensive cardiovascular sample data. Preparing samples for shipment.', '550e8400-e29b-41d4-a716-446655440002'),
       ('post-550e8400-e29b-41d4-a716-446655440003-001', '2024-01-04 11:15:00', '2024-01-04 11:15:00', 2, 2, 'PUBLIC', 'We are checking availability locally for this study. Our biobank is reviewing sample availability and will provide feedback shortly.', '550e8400-e29b-41d4-a716-446655440003');
