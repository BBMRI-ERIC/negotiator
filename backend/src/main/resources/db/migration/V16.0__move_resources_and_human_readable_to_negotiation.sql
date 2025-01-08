alter table negotiation
    add column human_readable       text,
    add column discovery_service_id bigint,
    add constraint fkey_discovery_service_id FOREIGN KEY (discovery_service_id) REFERENCES discovery_service (id);

create table negotiation_resource_link
(
    negotiation_id character varying(255) not null references negotiation (id),
    resource_id    bigint                 not null references resource (id),
    current_state  varchar(255),
    primary key (negotiation_id, resource_id)
);

update negotiation
set (human_readable, discovery_service_id) = (select human_readable, discovery_service_id
                                              from request
                                              where negotiation.id = request.negotiation_id);
update negotiation
set discovery_service_id = 1
where negotiation.discovery_service_id is null;
alter table negotiation
    alter column discovery_service_id set not null;
INSERT INTO negotiation_resource_link (negotiation_id, resource_id, current_state)
SELECT n.id, r.id, rspn.current_state
FROM resource_state_per_negotiation rspn
         JOIN public.resource r ON r.source_id = rspn.resource_id
         JOIN public.negotiation n ON n.id = rspn.negotiation_id
         JOIN public.request r2 ON n.id = r2.negotiation_id;
INSERT INTO negotiation_resource_link (negotiation_id, resource_id, current_state)
SELECT n.id, r2.id, NULL
FROM negotiation n
         JOIN public.request r ON n.id = r.negotiation_id
         JOIN public.request_resources_link rrl ON r.id = rrl.request_id
         JOIN public.resource r2 ON r2.id = rrl.resource_id
where n.current_state = 'SUBMITTED'
   or n.current_state = 'DECLINED';


alter table request
    drop column negotiation_id;
drop table resource_state_per_negotiation;