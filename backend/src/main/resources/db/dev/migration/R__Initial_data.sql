insert into discovery_service (id, url, name)
values (1, 'https://bbmritestnn.gcc.rug.nl', 'Biobank Directory');

insert into person (id, email, name, subject_id, password, organization, admin)
values (1, 'adam.researcher@gmail.com', 'TheResearcher', '1000@bbmri.eu', null, 'BBMRI', false),
       (2, 'taylor.biobanker@gmail.com', 'TheBiobanker', '1001@bbmri.eu', null, 'BBMRI', false),
       (3, 'for-backwards-compatability', 'directory', '',
        '$2y$10$6WCNhO3ppwgEN..vRrlQzuGv9Cng/eNIHjJx1vD2m8JfRWr6eMdFO', null, false),
       (4, 'sarah.johnson@gmail.com', 'SarahJohnson', '1002@bbmri.eu', null, 'BBMRI', false),
       (5, 'michael.chen@gmail.com', 'MichaelChen', '1003@bbmri.eu', null, 'BBMRI', false),
       (6, 'emma.williams@gmail.com', 'EmmaWilliams', '1004@bbmri.eu', null, 'BBMRI', false);

insert into organization (ID, external_id, name)
values (1, 'bbmri-eric:ID:SE_890', 'Biobank Väst'),
       (2, 'bbmri-eric:ID:CZ_MMCI', 'Masaryk Memorial Cancer Institute'),
       (3, 'bbmri-eric:ID:DE_MHH', 'Hannover Medical School Biobank'),
       (4, 'bbmri-eric:ID:FR_APHP', 'AP-HP Biobank Network'),
       (5, 'bbmri-eric:ID:IT_FPG', 'Italian Genome Foundation'),
       (6, 'bbmri-eric:ID:UK_BIOB', 'UK Biobank');

insert into resource (id, name, description, source_id, discovery_service_id, access_form_id, organization_id)
values (1, 'Dummy test collection', 'This is the fist test collection',
        'bbmri-eric:ID:SE_890:collection:dummy_collection', 1, 1, 1),
       (2, 'Test collection 2', 'This is the second test collection',
        'bbmri-eric:ID:CZ_MMCI:collection:LTS', 1, 3, 2),
       (3, 'German Cancer DNA Collection', 'Comprehensive DNA collection for cancer research',
        'bbmri-eric:ID:DE_MHH:collection:cancer_dna', 1, 1, 3),
       (4, 'French Plasma Repository', 'Large plasma sample repository for metabolic studies',
        'bbmri-eric:ID:FR_APHP:collection:plasma_repo', 1, 3, 4),
       (5, 'Italian Rare Disease Collection', 'Rare disease samples and genomic data',
        'bbmri-eric:ID:IT_FPG:collection:rare_disease', 1, 1, 5),
       (6, 'UK Cardiovascular Biobank', 'Cardiovascular disease samples and clinical data',
        'bbmri-eric:ID:UK_BIOB:collection:cardio', 1, 3, 6),
       (7, 'Swedish Tissue Bank', 'Tissue samples from various disease cohorts',
        'bbmri-eric:ID:SE_890:collection:tissue_bank', 1, 1, 1),
       (8, 'Czech Genomic Archive', 'Genomic DNA and RNA samples',
        'bbmri-eric:ID:CZ_MMCI:collection:genomic', 1, 3, 2);

insert into resource_representative_link (resource_id, person_id)
values (2, 2),
       (3, 4),
       (4, 5),
       (5, 6),
       (6, 2),
       (7, 4),
       (8, 5);

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

-- Negotiations
insert into negotiation (id, creation_date, current_state, modified_date, created_by, modified_by, human_readable, payload, private_posts_enabled, public_posts_enabled, discovery_service_id)
values ('a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d', '2024-10-12', 'IN_PROGRESS', '2024-10-12', 1, 1, '#1 Material Type: DNA',
        '{"project":{"title":"Biobanking research project","description":"A comprehensive study on DNA samples"},"samples":{"sample-type":"DNA","num-of-subjects": 150,"num-of-sample": "500","volume":5},"ethics-vote":{"ethics-vote":"Approved by ethics committee"}}',
        true, true, 1),
       ('b2c3d4e5-f6a7-4b8c-9d0e-1f2a3b4c5d6e', '2024-11-15', 'SUBMITTED', '2024-11-20', 1, 1, '#1 Material Type: Plasma',
        '{"project":{"title":"Cancer biomarker study","description":"Study on plasma biomarkers for early cancer detection"},"samples":{"sample-type":"Plasma","num-of-subjects": 200,"num-of-sample": "800","volume":3},"ethics-vote":{"ethics-vote":"Pending approval"}}',
        false, true, 1),
       ('c3d4e5f6-a7b8-4c9d-0e1f-2a3b4c5d6e7f', '2024-09-01', 'ABANDONED', '2024-09-15', 1, 1, '#1 Material Type: Blood',
        '{"project":{"title":"Cardiovascular disease study","description":"Research on cardiovascular risk factors"},"samples":{"sample-type":"Blood","num-of-subjects": 75,"num-of-sample": "300","volume":4},"ethics-vote":{"ethics-vote":"Ethics approval obtained"}}',
        false, false, 1),
       ('d4e5f6a7-b8c9-4d0e-1f2a-3b4c5d6e7f8a', '2024-01-05', 'CONCLUDED', '2024-03-10', 4, 4, '#1 Type: Cancer Research',
        '{"project":{"title":"Pancreatic cancer genomics","description":"Genomic profiling of pancreatic cancer patients"},"samples":{"sample-type":"Tissue","num-of-subjects": 100,"num-of-sample": "400","volume":2},"ethics-vote":{"ethics-vote":"Approved"}}',
        true, true, 1),
       ('e5f6a7b8-c9d0-4e1f-2a3b-4c5d6e7f8a9b', '2025-12-01', 'SUBMITTED', '2025-12-03', 4, 4, '#1 Material Type: RNA',
        '{"project":{"title":"RNA sequencing project","description":"Transcriptome analysis of rare diseases"},"samples":{"sample-type":"RNA","num-of-subjects": 50,"num-of-sample": "200","volume":1},"ethics-vote":{"ethics-vote":"Under review"}}',
        false, true, 1),
       ('f6a7b8c9-d0e1-4f2a-3b4c-5d6e7f8a9b0c', '2024-12-10', 'IN_PROGRESS', '2024-12-20', 5, 5, '#1 Type: Metabolic Disorders',
        '{"project":{"title":"Metabolic syndrome analysis","description":"Large-scale metabolomic study"},"samples":{"sample-type":"Plasma","num-of-subjects": 300,"num-of-sample": "1200","volume":5},"ethics-vote":{"ethics-vote":"Approved by IRB"}}',
        true, true, 1),
       ('a7b8c9d0-e1f2-4a3b-4c5d-6e7f8a9b0c1d', '2025-03-01', 'IN_PROGRESS', '2025-03-05', 1, 1, '#1 Type: Neurology',
        '{"project":{"title":"Alzheimer disease biobank","description":"Longitudinal study on Alzheimer progression"},"samples":{"sample-type":"CSF","num-of-subjects": 120,"num-of-sample": "480","volume":2},"ethics-vote":{"ethics-vote":"Approved"}}',
        true, true, 1),
       ('b8c9d0e1-f2a3-4b4c-5d6e-7f8a9b0c1d2e', '2024-08-15', 'ABANDONED', '2024-08-30', 4, 4, '#1 Material Type: Tissue',
        '{"project":{"title":"Abandoned tissue study","description":"This project was cancelled"},"samples":{"sample-type":"Tissue","num-of-subjects": 30,"num-of-sample": "120","volume":3},"ethics-vote":{"ethics-vote":"N/A"}}',
        false, false, 1),
       ('c9d0e1f2-a3b4-4c5d-6e7f-8a9b0c1d2e3f', '2024-02-20', 'CONCLUDED', '2024-05-01', 5, 5, '#1 Type: Pediatric Research',
        '{"project":{"title":"Childhood leukemia study","description":"Genetic factors in pediatric leukemia"},"samples":{"sample-type":"DNA","num-of-subjects": 80,"num-of-sample": "320","volume":2},"ethics-vote":{"ethics-vote":"Special pediatric approval"}}',
        true, true, 1),
       ('d0e1f2a3-b4c5-4d6e-7f8a-9b0c1d2e3f4a', '2026-01-05', 'SUBMITTED', '2026-01-06', 1, 1, '#1 Type: Infectious Disease',
        '{"project":{"title":"COVID-19 antibody research","description":"Longitudinal antibody response study"},"samples":{"sample-type":"Serum","num-of-subjects": 500,"num-of-sample": "2000","volume":3},"ethics-vote":{"ethics-vote":"Fast-track approved"}}',
        false, true, 1),
       ('e1f2a3b4-c5d6-4e7f-8a9b-0c1d2e3f4a5b', '2024-11-01', 'IN_PROGRESS', '2024-11-15', 4, 4, '#1 Material Type: DNA',
        '{"project":{"title":"Population genomics","description":"Large-scale population study"},"samples":{"sample-type":"DNA","num-of-subjects": 1000,"num-of-sample": "1000","volume":2},"ethics-vote":{"ethics-vote":"Multi-center approval"}}',
        true, true, 1),
       ('f2a3b4c5-d6e7-4f8a-9b0c-1d2e3f4a5b6c', '2024-03-10', 'CONCLUDED', '2024-07-25', 5, 5, '#1 Type: Diabetes',
        '{"project":{"title":"Type 2 diabetes cohort","description":"Genetic and environmental factors in T2D"},"samples":{"sample-type":"Blood","num-of-subjects": 250,"num-of-sample": "1000","volume":4},"ethics-vote":{"ethics-vote":"Approved"}}',
        true, true, 1),
       ('a3b4c5d6-e7f8-4a9b-0c1d-2e3f4a5b6c7d', '2026-01-10', 'DRAFT', '2026-01-10', 1, 1, '#1 Material Type: Various',
        '{"project":{"title":"Draft proposal","description":"Not yet completed"}}',
        false, true, 1),
       ('b4c5d6e7-f8a9-4b0c-1d2e-3f4a5b6c7d8e', '2024-10-01', 'IN_PROGRESS', '2024-10-15', 4, 4, '#1 Type: Lung Cancer',
        '{"project":{"title":"Non-small cell lung cancer","description":"NSCLC biomarker discovery"},"samples":{"sample-type":"Tissue","num-of-subjects": 150,"num-of-sample": "600","volume":3},"ethics-vote":{"ethics-vote":"Approved"}}',
        true, true, 1),
       ('c5d6e7f8-a9b0-4c1d-2e3f-4a5b6c7d8e9f', '2024-05-15', 'CONCLUDED', '2024-10-30', 5, 5, '#1 Type: Autoimmune',
        '{"project":{"title":"Rheumatoid arthritis study","description":"Immune profiling in RA patients"},"samples":{"sample-type":"Blood","num-of-subjects": 100,"num-of-sample": "400","volume":5},"ethics-vote":{"ethics-vote":"Approved by ethics board"}}',
        true, true, 1),
       ('d6e7f8a9-b0c1-4d2e-3f4a-5b6c7d8e9f0a', '2025-11-20', 'SUBMITTED', '2025-11-22', 1, 1, '#1 Material Type: Saliva',
        '{"project":{"title":"Oral microbiome study","description":"Microbiome diversity in health and disease"},"samples":{"sample-type":"Saliva","num-of-subjects": 200,"num-of-sample": "600","volume":2},"ethics-vote":{"ethics-vote":"Pending"}}',
        false, true, 1),
       ('e7f8a9b0-c1d2-4e3f-4a5b-6c7d8e9f0a1b', '2024-01-10', 'CONCLUDED', '2024-06-25', 4, 4, '#1 Type: Breast Cancer',
        '{"project":{"title":"Triple negative breast cancer","description":"Molecular characterization of TNBC"},"samples":{"sample-type":"Tissue","num-of-subjects": 90,"num-of-sample": "360","volume":2},"ethics-vote":{"ethics-vote":"Approved"}}',
        true, true, 1),
       ('f8a9b0c1-d2e3-4f4a-5b6c-7d8e9f0a1b2c', '2024-12-01', 'IN_PROGRESS', '2024-12-15', 5, 5, '#1 Type: Liver Disease',
        '{"project":{"title":"NASH progression study","description":"Non-alcoholic steatohepatitis biomarkers"},"samples":{"sample-type":"Liver Tissue","num-of-subjects": 60,"num-of-sample": "240","volume":3},"ethics-vote":{"ethics-vote":"Approved"}}',
        true, true, 1),
       ('a9b0c1d2-e3f4-4a5b-6c7d-8e9f0a1b2c3d', '2025-10-10', 'PAUSED', '2025-11-11', 1, 1, '#1 Type: Kidney Disease',
        '{"project":{"title":"Chronic kidney disease cohort","description":"Progression markers in CKD"},"samples":{"sample-type":"Urine","num-of-subjects": 180,"num-of-sample": "720","volume":4},"ethics-vote":{"ethics-vote":"Under review"}}',
        true, true, 1),
       ('b0c1d2e3-f4a5-4b6c-7d8e-9f0a1b2c3d4e', '2024-08-01', 'IN_PROGRESS', '2024-08-20', 4, 4, '#1 Type: Psychiatric Disorders',
        '{"project":{"title":"Depression biomarker study","description":"Biological markers of major depression"},"samples":{"sample-type":"Blood","num-of-subjects": 140,"num-of-sample": "560","volume":3},"ethics-vote":{"ethics-vote":"Approved with conditions"}}',
        true, true, 1);

-- Link negotiations to resources
insert into negotiation_resource_link (negotiation_id, resource_id, current_state)
values ('a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d', 1, 'REPRESENTATIVE_CONTACTED'),
       ('b2c3d4e5-f6a7-4b8c-9d0e-1f2a3b4c5d6e', 2, 'SUBMITTED'),
       ('c3d4e5f6-a7b8-4c9d-0e1f-2a3b4c5d6e7f', 1, null),
       ('d4e5f6a7-b8c9-4d0e-1f2a-3b4c5d6e7f8a', 3, 'RESOURCE_MADE_AVAILABLE'),
       ('e5f6a7b8-c9d0-4e1f-2a3b-4c5d6e7f8a9b', 5, 'SUBMITTED'),
       ('f6a7b8c9-d0e1-4f2a-3b4c-5d6e7f8a9b0c', 4, 'CHECKING_AVAILABILITY'),
       ('a7b8c9d0-e1f2-4a3b-4c5d-6e7f8a9b0c1d', 6, 'REPRESENTATIVE_CONTACTED'),
       ('b8c9d0e1-f2a3-4b4c-5d6e-7f8a9b0c1d2e', 7, null),
       ('c9d0e1f2-a3b4-4c5d-6e7f-8a9b0c1d2e3f', 3, 'RESOURCE_MADE_AVAILABLE'),
       ('d0e1f2a3-b4c5-4d6e-7f8a-9b0c1d2e3f4a', 2, 'SUBMITTED'),
       ('e1f2a3b4-c5d6-4e7f-8a9b-0c1d2e3f4a5b', 1, 'CHECKING_AVAILABILITY'),
       ('e1f2a3b4-c5d6-4e7f-8a9b-0c1d2e3f4a5b', 3, 'RESOURCE_AVAILABLE'),
       ('f2a3b4c5-d6e7-4f8a-9b0c-1d2e3f4a5b6c', 4, 'RESOURCE_MADE_AVAILABLE'),
       ('a3b4c5d6-e7f8-4a9b-0c1d-2e3f4a5b6c7d', 1, null),
       ('b4c5d6e7-f8a9-4b0c-1d2e-3f4a5b6c7d8e', 3, 'CHECKING_AVAILABILITY'),
       ('b4c5d6e7-f8a9-4b0c-1d2e-3f4a5b6c7d8e', 7, 'REPRESENTATIVE_CONTACTED'),
       ('c5d6e7f8-a9b0-4c1d-2e3f-4a5b6c7d8e9f', 5, 'RESOURCE_MADE_AVAILABLE'),
       ('d6e7f8a9-b0c1-4d2e-3f4a-5b6c7d8e9f0a', 2, 'SUBMITTED'),
       ('e7f8a9b0-c1d2-4e3f-4a5b-6c7d8e9f0a1b', 3, 'RESOURCE_MADE_AVAILABLE'),
       ('f8a9b0c1-d2e3-4f4a-5b6c-7d8e9f0a1b2c', 4, 'ACCESS_CONDITIONS_INDICATED'),
       ('a9b0c1d2-e3f4-4a5b-6c7d-8e9f0a1b2c3d', 6, 'REPRESENTATIVE_CONTACTED'),
       ('b0c1d2e3-f4a5-4b6c-7d8e-9f0a1b2c3d4e', 2, 'RESOURCE_AVAILABLE'),
       ('b0c1d2e3-f4a5-4b6c-7d8e-9f0a1b2c3d4e', 8, 'ACCESS_CONDITIONS_INDICATED');

-- Posts for negotiation-1 (5 posts - active discussion)
insert into post (id, creation_date, modified_date, text, created_by, modified_by, negotiation_id, organization_id, type)
values ('post-1-researcher', '2024-10-12 10:30:00', '2024-10-12 10:30:00', 'Hello, I am interested in accessing your DNA collection for our biobanking research project. Could you provide more details about availability?', 1, 1, 'a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d', null, 'PUBLIC'),
       ('post-2-representative', '2024-10-12 14:15:00', '2024-10-12 14:15:00', 'Thank you for your interest. We have DNA samples available that match your requirements. What is your timeline for the study?', 2, 2, 'a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d', null, 'PUBLIC'),
       ('post-3-researcher', '2024-10-13 09:00:00', '2024-10-13 09:00:00', 'We plan to start the study in Q1 2025. How many samples can you provide?', 1, 1, 'a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d', null, 'PUBLIC'),
       ('post-4-representative', '2024-10-13 11:20:00', '2024-10-13 11:20:00', 'We can provide up to 500 DNA samples. I will send you the detailed information privately.', 2, 2, 'a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d', null, 'PUBLIC'),
       ('post-5-representative-private', '2024-10-13 11:25:00', '2024-10-13 11:25:00', 'Here are the specific details about sample quality, storage conditions, and access procedures.', 2, 2, 'a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d', 1, 'PRIVATE');

-- Posts for negotiation-2 (2 posts - initial contact)
insert into post (id, creation_date, modified_date, text, created_by, modified_by, negotiation_id, organization_id, type)
values ('post-6-researcher', '2024-11-15 08:00:00', '2024-11-15 08:00:00', 'We are conducting a cancer biomarker study and would like to request access to plasma samples.', 1, 1, 'b2c3d4e5-f6a7-4b8c-9d0e-1f2a3b4c5d6e', null, 'PUBLIC'),
       ('post-7-representative', '2024-11-16 10:30:00', '2024-11-16 10:30:00', 'Your request has been received. We will review it and get back to you within 5 business days.', 2, 2, 'b2c3d4e5-f6a7-4b8c-9d0e-1f2a3b4c5d6e', null, 'PUBLIC');

-- Posts for negotiation-4 (8 posts - extensive discussion)
insert into post (id, creation_date, modified_date, text, created_by, modified_by, negotiation_id, organization_id, type)
values ('post-8', '2025-01-05 09:00:00', '2025-01-05 09:00:00', 'Starting new pancreatic cancer genomics project. Looking for fresh frozen tissue samples.', 4, 4, 'd4e5f6a7-b8c9-4d0e-1f2a-3b4c5d6e7f8a', null, 'PUBLIC'),
       ('post-9', '2025-01-05 14:30:00', '2025-01-05 14:30:00', 'We have a collection that might be suitable. Can you provide more details about your requirements?', 4, 4, 'd4e5f6a7-b8c9-4d0e-1f2a-3b4c5d6e7f8a', null, 'PUBLIC'),
       ('post-10', '2025-01-06 10:15:00', '2025-01-06 10:15:00', 'We need samples from treatment-naive patients with complete clinical follow-up data.', 4, 4, 'd4e5f6a7-b8c9-4d0e-1f2a-3b4c5d6e7f8a', null, 'PUBLIC'),
       ('post-11', '2025-01-07 11:00:00', '2025-01-07 11:00:00', 'That matches our collection criteria. We have approximately 120 samples available.', 4, 4, 'd4e5f6a7-b8c9-4d0e-1f2a-3b4c5d6e7f8a', null, 'PUBLIC'),
       ('post-12', '2025-01-07 15:20:00', '2025-01-07 15:20:00', 'Excellent! What is the procedure for data access?', 4, 4, 'd4e5f6a7-b8c9-4d0e-1f2a-3b4c5d6e7f8a', null, 'PUBLIC'),
       ('post-13', '2025-01-08 09:30:00', '2025-01-08 09:30:00', 'You will need to submit a formal application with your ethics approval. I am sending the template privately.', 4, 4, 'd4e5f6a7-b8c9-4d0e-1f2a-3b4c5d6e7f8a', null, 'PUBLIC'),
       ('post-14', '2025-01-08 09:35:00', '2025-01-08 09:35:00', 'Here is the application template and access agreement.', 4, 4, 'd4e5f6a7-b8c9-4d0e-1f2a-3b4c5d6e7f8a', 3, 'PRIVATE'),
       ('post-15', '2025-01-10 13:00:00', '2025-01-10 13:00:00', 'Thank you! We will prepare the application and send it by end of week.', 4, 4, 'd4e5f6a7-b8c9-4d0e-1f2a-3b4c5d6e7f8a', null, 'PUBLIC');

-- Posts for negotiation-5 (3 posts)
insert into post (id, creation_date, modified_date, text, created_by, modified_by, negotiation_id, organization_id, type)
values ('post-16', '2025-02-01 08:00:00', '2025-02-01 08:00:00', 'Request for RNA samples for rare disease transcriptome analysis.', 4, 4, 'e5f6a7b8-c9d0-4e1f-2a3b-4c5d6e7f8a9b', null, 'PUBLIC'),
       ('post-17', '2025-02-02 10:00:00', '2025-02-02 10:00:00', 'We have RNA samples from 35 different rare diseases. Which ones are you interested in?', 6, 6, 'e5f6a7b8-c9d0-4e1f-2a3b-4c5d6e7f8a9b', null, 'PUBLIC'),
       ('post-18', '2025-02-03 11:30:00', '2025-02-03 11:30:00', 'Primarily interested in lysosomal storage disorders and mitochondrial diseases.', 4, 4, 'e5f6a7b8-c9d0-4e1f-2a3b-4c5d6e7f8a9b', null, 'PUBLIC');

-- Posts for negotiation-6 (1 post - minimal)
insert into post (id, creation_date, modified_date, text, created_by, modified_by, negotiation_id, organization_id, type)
values ('post-19', '2024-12-10 09:00:00', '2024-12-10 09:00:00', 'Request approved for metabolic syndrome plasma samples. Access details will be sent separately.', 5, 5, 'f6a7b8c9-d0e1-4f2a-3b4c-5d6e7f8a9b0c', null, 'PUBLIC');

-- Posts for negotiation-7 (10 posts - very active)
insert into post (id, creation_date, modified_date, text, created_by, modified_by, negotiation_id, organization_id, type)
values ('post-20', '2025-03-01 08:00:00', '2025-03-01 08:00:00', 'Starting longitudinal Alzheimer study. Need CSF samples from early-stage patients.', 1, 1, 'a7b8c9d0-e1f2-4a3b-4c5d-6e7f8a9b0c1d', null, 'PUBLIC'),
       ('post-21', '2025-03-01 10:30:00', '2025-03-01 10:30:00', 'This is a very interesting project. We have a well-characterized cohort.', 2, 2, 'a7b8c9d0-e1f2-4a3b-4c5d-6e7f8a9b0c1d', null, 'PUBLIC'),
       ('post-22', '2025-03-01 14:00:00', '2025-03-01 14:00:00', 'How many subjects are in your cohort?', 1, 1, 'a7b8c9d0-e1f2-4a3b-4c5d-6e7f8a9b0c1d', null, 'PUBLIC'),
       ('post-23', '2025-03-02 09:15:00', '2025-03-02 09:15:00', 'We have 85 subjects with longitudinal CSF samples (2-5 timepoints each).', 2, 2, 'a7b8c9d0-e1f2-4a3b-4c5d-6e7f8a9b0c1d', null, 'PUBLIC'),
       ('post-24', '2025-03-02 11:00:00', '2025-03-02 11:00:00', 'Perfect! What biomarkers have already been measured?', 1, 1, 'a7b8c9d0-e1f2-4a3b-4c5d-6e7f8a9b0c1d', null, 'PUBLIC'),
       ('post-25', '2025-03-02 15:30:00', '2025-03-02 15:30:00', 'Standard AD biomarkers: Aβ42, t-tau, p-tau. Also have MRI and cognitive assessment data.', 2, 2, 'a7b8c9d0-e1f2-4a3b-4c5d-6e7f8a9b0c1d', null, 'PUBLIC'),
       ('post-26', '2025-03-03 10:00:00', '2025-03-03 10:00:00', 'Excellent. We would like to measure additional inflammatory markers. What volume is available per sample?', 1, 1, 'a7b8c9d0-e1f2-4a3b-4c5d-6e7f8a9b0c1d', null, 'PUBLIC'),
       ('post-27', '2025-03-03 13:45:00', '2025-03-03 13:45:00', 'Typically 0.5-1 mL per aliquot. We can provide 2-3 aliquots per timepoint.', 2, 2, 'a7b8c9d0-e1f2-4a3b-4c5d-6e7f8a9b0c1d', null, 'PUBLIC'),
       ('post-28', '2025-03-04 09:00:00', '2025-03-04 09:00:00', 'That should be sufficient for our multiplex assays. What are the next steps?', 1, 1, 'a7b8c9d0-e1f2-4a3b-4c5d-6e7f8a9b0c1d', null, 'PUBLIC'),
       ('post-29', '2025-03-05 11:00:00', '2025-03-05 11:00:00', 'Please submit your detailed project proposal and we will schedule a committee review.', 2, 2, 'a7b8c9d0-e1f2-4a3b-4c5d-6e7f8a9b0c1d', null, 'PUBLIC');

-- Posts for negotiation-9 (4 posts)
insert into post (id, creation_date, modified_date, text, created_by, modified_by, negotiation_id, organization_id, type)
values ('post-30', '2024-07-20 10:00:00', '2024-07-20 10:00:00', 'Requesting access to pediatric leukemia samples for genetic analysis.', 5, 5, 'c9d0e1f2-a3b4-4c5d-6e7f-8a9b0c1d2e3f', null, 'PUBLIC'),
       ('post-31', '2024-07-22 11:30:00', '2024-07-22 11:30:00', 'We need to verify your pediatric ethics approval before proceeding.', 4, 4, 'c9d0e1f2-a3b4-4c5d-6e7f-8a9b0c1d2e3f', null, 'PUBLIC'),
       ('post-32', '2024-07-25 09:00:00', '2024-07-25 09:00:00', 'Ethics documentation has been submitted through the portal.', 5, 5, 'c9d0e1f2-a3b4-4c5d-6e7f-8a9b0c1d2e3f', null, 'PUBLIC'),
       ('post-33', '2024-08-01 14:00:00', '2024-08-01 14:00:00', 'Approved! Samples will be shipped next week.', 4, 4, 'c9d0e1f2-a3b4-4c5d-6e7f-8a9b0c1d2e3f', null, 'PUBLIC');

-- Posts for negotiation-11 (6 posts)
insert into post (id, creation_date, modified_date, text, created_by, modified_by, negotiation_id, organization_id, type)
values ('post-34', '2024-11-01 08:30:00', '2024-11-01 08:30:00', 'Large population genomics study - need 1000 DNA samples from diverse backgrounds.', 4, 4, 'e1f2a3b4-c5d6-4e7f-8a9b-0c1d2e3f4a5b', null, 'PUBLIC'),
       ('post-35', '2024-11-02 10:00:00', '2024-11-02 10:00:00', 'We have samples but will need to combine collections from multiple sites. Are you open to multi-center collaboration?', 2, 2, 'e1f2a3b4-c5d6-4e7f-8a9b-0c1d2e3f4a5b', null, 'PUBLIC'),
       ('post-36', '2024-11-03 09:15:00', '2024-11-03 09:15:00', 'Yes, absolutely. That would actually enhance the diversity of our cohort.', 4, 4, 'e1f2a3b4-c5d6-4e7f-8a9b-0c1d2e3f4a5b', null, 'PUBLIC'),
       ('post-37', '2024-11-05 14:00:00', '2024-11-05 14:00:00', 'Great! I will coordinate with two other biobanks. Expected total: 1200 samples.', 2, 2, 'e1f2a3b4-c5d6-4e7f-8a9b-0c1d2e3f4a5b', null, 'PUBLIC'),
       ('post-38', '2024-11-10 11:30:00', '2024-11-10 11:30:00', 'Perfect! Please send the breakdown by ethnicity and age groups.', 4, 4, 'e1f2a3b4-c5d6-4e7f-8a9b-0c1d2e3f4a5b', null, 'PUBLIC'),
       ('post-39', '2024-11-15 13:00:00', '2024-11-15 13:00:00', 'Detailed breakdown sent to your email. Let me know if you need any adjustments.', 2, 2, 'e1f2a3b4-c5d6-4e7f-8a9b-0c1d2e3f4a5b', null, 'PUBLIC');

-- Posts for negotiation-12 (2 posts)
insert into post (id, creation_date, modified_date, text, created_by, modified_by, negotiation_id, organization_id, type)
values ('post-40', '2024-06-10 09:00:00', '2024-06-10 09:00:00', 'Type 2 diabetes cohort study approved. Awaiting sample shipment schedule.', 5, 5, 'f2a3b4c5-d6e7-4f8a-9b0c-1d2e3f4a5b6c', null, 'PUBLIC'),
       ('post-41', '2024-06-25 10:30:00', '2024-06-25 10:30:00', 'Samples will be shipped in 3 batches: July 1, July 15, and August 1.', 5, 5, 'f2a3b4c5-d6e7-4f8a-9b0c-1d2e3f4a5b6c', null, 'PUBLIC');

-- Posts for negotiation-14 (7 posts)
insert into post (id, creation_date, modified_date, text, created_by, modified_by, negotiation_id, organization_id, type)
values ('post-42', '2024-10-01 08:00:00', '2024-10-01 08:00:00', 'Looking for NSCLC tissue samples with complete staging information.', 4, 4, 'b4c5d6e7-f8a9-4b0c-1d2e-3f4a5b6c7d8e', null, 'PUBLIC'),
       ('post-43', '2024-10-02 10:30:00', '2024-10-02 10:30:00', 'We have a large NSCLC collection. What stages are you focusing on?', 4, 4, 'b4c5d6e7-f8a9-4b0c-1d2e-3f4a5b6c7d8e', null, 'PUBLIC'),
       ('post-44', '2024-10-03 09:00:00', '2024-10-03 09:00:00', 'Primarily stage II and III. Also need matched normal tissue.', 4, 4, 'b4c5d6e7-f8a9-4b0c-1d2e-3f4a5b6c7d8e', null, 'PUBLIC'),
       ('post-45', '2024-10-05 11:00:00', '2024-10-05 11:00:00', 'We can provide 90 tumor samples with matched normals. All stage II-III.', 4, 4, 'b4c5d6e7-f8a9-4b0c-1d2e-3f4a5b6c7d8e', null, 'PUBLIC'),
       ('post-46', '2024-10-07 14:00:00', '2024-10-07 14:00:00', 'Excellent! What is the quality of RNA from these samples?', 4, 4, 'b4c5d6e7-f8a9-4b0c-1d2e-3f4a5b6c7d8e', null, 'PUBLIC'),
       ('post-47', '2024-10-10 10:00:00', '2024-10-10 10:00:00', 'RIN values range from 6.5 to 9.2. Median RIN is 7.8. Suitable for RNA-seq.', 4, 4, 'b4c5d6e7-f8a9-4b0c-1d2e-3f4a5b6c7d8e', null, 'PUBLIC'),
       ('post-48', '2024-10-15 13:30:00', '2024-10-15 13:30:00', 'Perfect for our needs. Proceeding with formal application.', 4, 4, 'b4c5d6e7-f8a9-4b0c-1d2e-3f4a5b6c7d8e', null, 'PUBLIC');

-- Posts for negotiation-15 (5 posts)
insert into post (id, creation_date, modified_date, text, created_by, modified_by, negotiation_id, organization_id, type)
values ('post-49', '2024-09-15 09:00:00', '2024-09-15 09:00:00', 'RA immune profiling study - need blood samples from active disease patients.', 5, 5, 'c5d6e7f8-a9b0-4c1d-2e3f-4a5b6c7d8e9f', null, 'PUBLIC'),
       ('post-50', '2024-09-17 10:30:00', '2024-09-17 10:30:00', 'We have samples from patients at different disease activity levels. Need specific DAS28 scores?', 6, 6, 'c5d6e7f8-a9b0-4c1d-2e3f-4a5b6c7d8e9f', null, 'PUBLIC'),
       ('post-51', '2024-09-20 11:00:00', '2024-09-20 11:00:00', 'Yes, preferably DAS28 > 5.1 (high disease activity).', 5, 5, 'c5d6e7f8-a9b0-4c1d-2e3f-4a5b6c7d8e9f', null, 'PUBLIC'),
       ('post-52', '2024-09-25 14:00:00', '2024-09-25 14:00:00', 'We have 65 samples meeting this criteria. All treatment-naive.', 6, 6, 'c5d6e7f8-a9b0-4c1d-2e3f-4a5b6c7d8e9f', null, 'PUBLIC'),
       ('post-53', '2024-09-30 10:00:00', '2024-09-30 10:00:00', 'Approved! Looking forward to collaboration.', 5, 5, 'c5d6e7f8-a9b0-4c1d-2e3f-4a5b6c7d8e9f', null, 'PUBLIC');

-- Posts for negotiation-17 (3 posts)
insert into post (id, creation_date, modified_date, text, created_by, modified_by, negotiation_id, organization_id, type)
values ('post-54', '2024-05-10 08:00:00', '2024-05-10 08:00:00', 'Triple negative breast cancer molecular study. Request for FFPE blocks.', 4, 4, 'e7f8a9b0-c1d2-4e3f-4a5b-6c7d8e9f0a1b', null, 'PUBLIC'),
       ('post-55', '2024-05-15 10:00:00', '2024-05-15 10:00:00', 'FFPE blocks available with 10+ year follow-up data. Will prepare material transfer agreement.', 4, 4, 'e7f8a9b0-c1d2-4e3f-4a5b-6c7d8e9f0a1b', null, 'PUBLIC'),
       ('post-56', '2024-05-25 09:00:00', '2024-05-25 09:00:00', 'MTA signed and returned. Ready to proceed.', 4, 4, 'e7f8a9b0-c1d2-4e3f-4a5b-6c7d8e9f0a1b', null, 'PUBLIC');

-- Posts for negotiation-18 (9 posts - extensive)
insert into post (id, creation_date, modified_date, text, created_by, modified_by, negotiation_id, organization_id, type)
values ('post-57', '2024-12-01 08:00:00', '2024-12-01 08:00:00', 'NASH biomarker study. Need liver biopsies with histological scoring.', 5, 5, 'f8a9b0c1-d2e3-4f4a-5b6c-7d8e9f0a1b2c', null, 'PUBLIC'),
       ('post-58', '2024-12-02 10:00:00', '2024-12-02 10:00:00', 'We have biopsies with complete NAS scoring. What fibrosis stages do you need?', 5, 5, 'f8a9b0c1-d2e3-4f4a-5b6c-7d8e9f0a1b2c', null, 'PUBLIC'),
       ('post-59', '2024-12-03 09:30:00', '2024-12-03 09:30:00', 'All stages, but particularly interested in F2-F3 (significant fibrosis).', 5, 5, 'f8a9b0c1-d2e3-4f4a-5b6c-7d8e9f0a1b2c', null, 'PUBLIC'),
       ('post-60', '2024-12-05 11:00:00', '2024-12-05 11:00:00', 'We have 45 F2-F3 samples. Also have paired serum samples if interested.', 5, 5, 'f8a9b0c1-d2e3-4f4a-5b6c-7d8e9f0a1b2c', null, 'PUBLIC'),
       ('post-61', '2024-12-06 14:00:00', '2024-12-06 14:00:00', 'Yes! The serum samples would be very valuable for validation.', 5, 5, 'f8a9b0c1-d2e3-4f4a-5b6c-7d8e9f0a1b2c', null, 'PUBLIC'),
       ('post-62', '2024-12-08 10:30:00', '2024-12-08 10:30:00', 'Great. I will include both tissue and serum in the proposal.', 5, 5, 'f8a9b0c1-d2e3-4f4a-5b6c-7d8e9f0a1b2c', null, 'PUBLIC'),
       ('post-63', '2024-12-10 09:00:00', '2024-12-10 09:00:00', 'What proteomic data is already available for these samples?', 5, 5, 'f8a9b0c1-d2e3-4f4a-5b6c-7d8e9f0a1b2c', null, 'PUBLIC'),
       ('post-64', '2024-12-12 11:30:00', '2024-12-12 11:30:00', 'We have run standard clinical chemistry but no proteomics yet.', 5, 5, 'f8a9b0c1-d2e3-4f4a-5b6c-7d8e9f0a1b2c', null, 'PUBLIC'),
       ('post-65', '2024-12-15 13:00:00', '2024-12-15 13:00:00', 'Perfect. This will be a good discovery cohort then.', 5, 5, 'f8a9b0c1-d2e3-4f4a-5b6c-7d8e9f0a1b2c', null, 'PUBLIC');

-- Posts for negotiation-19 (2 posts)
insert into post (id, creation_date, modified_date, text, created_by, modified_by, negotiation_id, organization_id, type)
values ('post-66', '2025-03-10 08:00:00', '2025-03-10 08:00:00', 'CKD progression study - requesting urine samples from stage 3-4 patients.', 1, 1, 'a9b0c1d2-e3f4-4a5b-6c7d-8e9f0a1b2c3d', null, 'PUBLIC'),
       ('post-67', '2025-03-11 10:00:00', '2025-03-11 10:00:00', 'Request received. Our scientific committee will review next week.', 2, 2, 'a9b0c1d2-e3f4-4a5b-6c7d-8e9f0a1b2c3d', null, 'PUBLIC');

-- Posts for negotiation-20 (4 posts)
insert into post (id, creation_date, modified_date, text, created_by, modified_by, negotiation_id, organization_id, type)
values ('post-68', '2024-08-01 09:00:00', '2024-08-01 09:00:00', 'Depression biomarker study - need blood samples from MDD patients before treatment.', 4, 4, 'b0c1d2e3-f4a5-4b6c-7d8e-9f0a1b2c3d4e', null, 'PUBLIC'),
       ('post-69', '2024-08-05 10:30:00', '2024-08-05 10:30:00', 'We have pre-treatment samples from 95 MDD patients. All with HAM-D scores > 18.', 2, 2, 'b0c1d2e3-f4a5-4b6c-7d8e-9f0a1b2c3d4e', null, 'PUBLIC'),
       ('post-70', '2024-08-10 11:00:00', '2024-08-10 11:00:00', 'Excellent! Do you also have samples post-treatment for comparison?', 4, 4, 'b0c1d2e3-f4a5-4b6c-7d8e-9f0a1b2c3d4e', null, 'PUBLIC'),
       ('post-71', '2024-08-20 14:00:00', '2024-08-20 14:00:00', 'Yes, we have 8-week follow-up samples for 78 of these patients. Approved for access!', 2, 2, 'b0c1d2e3-f4a5-4b6c-7d8e-9f0a1b2c3d4e', null, 'PUBLIC');

