alter table negotiation
add column human_readable text,
add column discovery_service_id bigint,
add constraint fkey_discovery_service_id FOREIGN KEY (discovery_service_id) REFERENCES discovery_service(id);

create table negotiation_resource_link (
    negotiation_id character varying(255) not null references negotiation(id),
    resource_id bigint not null references resource(id),
    current_state varchar(255),
    primary key (negotiation_id, resource_id)
);

update negotiation
set (human_readable, discovery_service_id) = (
	select human_readable, discovery_service_id
	from request
	where negotiation.id = request.negotiation_id
);

alter table negotiation alter column human_readable set not null;
alter table negotiation alter column discovery_service_id set not null;

insert into negotiation_resource_link (negotiation_id, resource_id)
select r.negotiation_id, rrl.resource_id from request_resources_link rrl join request r on rrl.request_id = r.id;

delete from request where negotiation_id is null;
alter table request drop column negotiation_id;

