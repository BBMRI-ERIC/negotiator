alter table data_source rename to discovery_service;

alter table request rename column data_source_id to discovery_service_id;
alter table resource rename column data_source_id to discovery_service_id;


alter table discovery_service drop column sync_active;
alter table discovery_service drop column api_password;
alter table discovery_service drop column api_type;
alter table discovery_service drop column api_url;
alter table discovery_service drop column api_username;
alter table discovery_service drop column description;
alter table discovery_service drop column resource_biobank;
alter table discovery_service drop column resource_collection;
alter table discovery_service drop column resource_network;
alter table discovery_service drop column source_prefix;
