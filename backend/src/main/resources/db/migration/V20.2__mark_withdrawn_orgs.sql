update organization
set withdrawn = false
where withdrawn is null;
alter table organization
    alter column withdrawn set not null;