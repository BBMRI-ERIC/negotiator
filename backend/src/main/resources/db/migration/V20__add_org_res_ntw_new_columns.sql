alter table organization add column contact_email varchar(255);

alter table organization add column uri varchar(255);

alter table organization add column description varchar(5000);

alter table resource add column contact_email varchar(255);

alter table resource add column uri varchar(255);

alter table resource
    add column withdrawn bool DEFAULT false;

alter table network add column description varchar(5000);
