alter table negotiation
add column human_readable text not null default '',
add column discovery_service_id bigint not null default 1,
add constraint fkey_discovery_service_id FOREIGN KEY (discovery_service_id) REFERENCES discovery_service(id);

create table negotiation_resources_link (
    negotiation_id character varying(255) not null references negotiation(id),
    resource_id bigint not null references resource(id),
    primary key (negotiation_id, resource_id)
);

update negotiation
set (human_readable, discovery_service_id) = (
	select human_readable, discovery_service_id
	from request
	where negotiation.id = request.negotiation_id
);

insert into negotiation_resources_link (negotiation_id, resource_id)
select r.negotiation_id, rrl.resource_id from request_resources_link rrl join request r on rrl.request_id = r.id;

delete from request where negotiation_id is null;

alter table request drop column negotiation_id;