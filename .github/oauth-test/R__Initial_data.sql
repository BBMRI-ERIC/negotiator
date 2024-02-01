-- adds the test directory as data source --
do $$
declare
   v_data_source data_source%rowtype;
begin
    select * from data_source
    into v_data_source
    where url='https://bbmritestnn.gcc.rug.nl';

  if not found then
    insert into data_source (url, api_username, api_password, api_type, api_url, description, name,
               resource_biobank, resource_collection, resource_network, source_prefix, sync_active)
    values ('https://bbmritestnn.gcc.rug.nl', 'user', 'password', 'MOLGENIS', 'https://bbmritestnn.gcc.rug.nl',
            'Biobank Directory', 'Biobank Directory', 'directory_biobanks', 'directory_collections',
            'directory_networks', 'source_prefix', 'false');
  end if;
end $$;

-- adds the default access criteria --
do $$
declare
   v_acs access_criteria_set%rowtype;
begin
    select * from access_criteria_set
    into v_acs
    where name = 'BBMRI Template';

    if not found then
        insert into access_criteria_set (id, name) values (1, 'BBMRI Template');
        insert into access_criteria_section (id, name, label, description, access_criteria_set_id)
        values (1, 'project', 'Project', 'Provide information about your project', 1);
        insert into access_criteria_section (id, name, label, description, access_criteria_set_id)
        values (2, 'request', 'Request', 'Provide information the resources you are requesting', 1);
        insert into access_criteria_section (id, name, label, description, access_criteria_set_id)
        values (3, 'ethics-vote', 'Ethics vote', 'Is ethics vote present in your project?', 1);
        insert into access_criteria (id, name, label, description, type)
        values (1, 'title', 'Title', 'Give a title', 'text');
        insert into access_criteria (id, name, label, description, type)
        values (2, 'description', 'Description', 'Give a description', 'textarea');
        insert into access_criteria (id, name, label, description, type)
        values (3, 'ethics-vote', 'Ethics vote', 'Write the etchics vote', 'textarea');
        insert into access_criteria (id, name, label, description, type)
        values (4, 'ethics-vote-attachment', 'Attachment', 'Upload Ethics Vote', 'file');
        insert into access_criteria_section_link (access_criteria_section_id, access_criteria_id, ordering, required)
        values (1, 1, 1, 'true');
        insert into access_criteria_section_link (access_criteria_section_id, access_criteria_id, ordering, required)
        values (1, 2, 2, 'false');
        insert into access_criteria_section_link (access_criteria_section_id, access_criteria_id, ordering, required)
        values (2, 2, 1, 'true');
        insert into access_criteria_section_link (access_criteria_section_id, access_criteria_id, ordering, required)
        values (3, 3, 1, 'false');
        insert into access_criteria_section_link (access_criteria_section_id, access_criteria_id, ordering, required)
        values (3, 4, 2, 'false');
  end if;
end $$;