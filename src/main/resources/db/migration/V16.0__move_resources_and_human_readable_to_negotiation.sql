alter table negotiation
add column human_readable text not null default = ''

create table negotiation_resources_link (
    negotiation_id character varying(255) not null references negotiation(id),
    resource_id bigint not null references resource(id),
    primary key (negotiation_id, resource_id)
)

update negotiation
set human_readable = (
	select human_readable
	from request
	where negotiation.id = request.negotiation_id
)

insert into negotiation_resources_link (negotiation_id, resource_id)
select r.negotiation_id, rrl.resource_id from request_resources_link rrl join request r on rrl.request_id = r.id
