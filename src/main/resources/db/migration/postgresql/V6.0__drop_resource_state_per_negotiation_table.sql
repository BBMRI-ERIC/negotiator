drop table if exists public.resource_state_per_negotiation;
alter table public.resource add constraint UC_SOURCE_ID unique (source_id);