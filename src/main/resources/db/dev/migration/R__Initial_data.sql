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

insert into public.ui_parameter (id, category, name, type, value) values
  (1, 'theme', 'primaryColor', 'STRING', '#26336B'),
  (2, 'theme', 'secondaryColor', 'STRING', '#26336B'),
  (3, 'theme', 'primaryTextColor', 'STRING', '#3c3c3d'),
  (4, 'theme', 'secondaryTextColor', 'STRING', '#3c3c3d'),
  (5, 'theme', 'appBackgroundColor', 'STRING', '#ffffff'),
  (6, 'theme', 'LinksTextColor', 'STRING', '#3c3c3d'),
  (7, 'theme', 'LinksColor', 'STRING', '#26336B'),
  (8, 'theme', 'ButtonColor', 'STRING', '#26336B'),
  (9, 'navbar', 'navbarLogoUrl', 'STRING', 'bbmri'),
  (10, 'navbar', 'navbarBackgroundColor', 'STRING', '#e7e7e7'),
  (11, 'navbar', 'navbarTextColor', 'STRING', '#3c3c3d'),
  (12, 'navbar', 'navbarActiveTextColor', 'STRING', '#e95713'),
  (13, 'navbar', 'navbarWelcomeTextColor', 'STRING', '#3c3c3d'),
  (14, 'navbar', 'navbarButtonOutlineColor', 'STRING', '#003674'),
  (15, 'footer', 'isFooterLeftSideIconVisible', 'BOOL', true),
  (16, 'footer', 'footerLeftSideIcon', 'STRING', 'bbmri'),
  (17, 'footer', 'footerLeftSideIconLink', 'STRING', 'https://www.bbmri-eric.eu/'),
  (18, 'footer', 'isFooterFollowUsVisible', 'BOOL', true),
  (19, 'footer', 'footerFollowUsLinkedin', 'STRING', 'https://www.linkedin.com/company/bbmri-eric'),
  (20, 'footer', 'footerFollowUsX', 'STRING', 'https://twitter.com/BBMRIERIC'),
  (21, 'footer', 'footerFollowUsPodcast', 'STRING', 'https://www.bbmri-eric.eu/bbmri-eric/bbmri-eric-podcast/'),
  (22, 'footer', 'isFooterGithubVisible', 'BOOL', true),
  (23, 'footer', 'footerGithubFrontendLink', 'STRING', 'https://github.com/BBMRI-ERIC/negotiator-v3-frontend'),
  (24, 'footer', 'footerGithubBackendLink', 'STRING', 'https://github.com/BBMRI-ERIC/negotiator'),
  (25, 'footer', 'isFooterSwaggerVisible', 'BOOL', true),
  (26, 'footer', 'footerSwaggerLink', 'STRING', '/api/swagger-ui/index.html'),
  (27, 'footer', 'footerSwaggerText', 'STRING', 'API'),
  (28, 'footer', 'isFooterStatusPageVisible', 'BOOL', true),
  (29, 'footer', 'footerStatusPageText', 'STRING', 'BBMRI-ERIC Status Page'),
  (30, 'footer', 'footerStatusPageLink', 'STRING', 'https://status.bbmri-eric.eu/'),
  (31, 'footer', 'isFooterWorkProgrammeVisible', 'BOOL', true),
  (32, 'footer', 'footerWorkProgrammeLink', 'STRING', 'https://www.bbmri-eric.eu/wp-content/uploads/BBMRI-ERIC_work-program_2022-2024_DIGITAL.pdf'),
  (33, 'footer', 'footerWorkProgrammeImageUrl', 'STRING', 'workProgramme'),
  (34, 'footer', 'footerWorkProgrammeText', 'STRING', 'Work Programme'),
  (35, 'footer', 'isFooterNewsletterVisible', 'BOOL', true),
  (36, 'footer', 'footerNewsletterLink', 'STRING', 'https://www.bbmri-eric.eu/news-event/'),
  (37, 'footer', 'footerNewsletterText', 'STRING', 'Subscribe To Our Newsletter'),
  (38, 'footer', 'isFooterPrivacyPolicyVisible', 'BOOL', true),
  (39, 'footer', 'footerPrivacyPolicyLink', 'STRING', 'https://www.bbmri-eric.eu/wp-content/uploads/AoM_10_8_Access-Policy_FINAL_EU.pdf'),
  (40, 'footer', 'footerPrivacyPolicyText', 'STRING', 'Privacy Policy'),
  (41, 'footer', 'isFooterCopyRightVisible', 'BOOL', true),
  (42, 'footer', 'footerCopyRightText', 'STRING', '© 2024 BBMRI-ERIC'),
  (43, 'footer', 'isFooterHelpLinkVisible', 'BOOL', true),
  (44, 'footer', 'footerHelpLink', 'STRING', 'mailto:negotiator@helpdesk.bbmri-eric.eu'),
  (45, 'footer', 'footerTextColor', 'STRING', '#3c3c3d'),
  (46, 'footer', 'footerNewsletterButtonColor', 'STRING', '#e7e7e7'),
  (47, 'login', 'loginLogoUrl', 'STRING', 'bbmri'),
  (48, 'login', 'loginNegotiatorTextColor', 'STRING', '#4d4d4f'),
  (49, 'login', 'loginTextColor', 'STRING', '#26336B'),
  (50, 'login', 'loginLinksTextColor', 'STRING', '#3c3c3d'),
  (51, 'login', 'loginLinksColor', 'STRING', '#26336B'),
  (52, 'login', 'logincardColor', 'STRING', '#ffffff'),
  (53, 'filtersSort', 'filtersSortButtonColor', 'STRING', '#003674'),
  (54, 'filtersSort', 'filtersSortDropdownTextColor', 'STRING', '#3c3c3d'),
  (55, 'filtersSort', 'filtersSortClearButtonColor', 'STRING', '#dc3545'),
  (56, 'negotiationList', 'searchResultsTextColor', 'STRING', '#3c3c3d'),
  (57, 'negotiationList', 'displayViewButtonColor', 'STRING', '#f37125'),
  (58, 'negotiationList', 'cardTextColor', 'STRING', '#3c3c3d'),
  (59, 'negotiationList', 'tableTextColor', 'STRING', '#3c3c3d');


