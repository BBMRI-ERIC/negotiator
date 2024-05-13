create table directory_sync_job_record (
  created_by bigint,
  creation_date timestamp(6),
  modified_by bigint,
  modified_date timestamp(6),
  job_state varchar(255) check (job_state in ('SUBMITTED','IN_PROGRESS', 'FAILED','COMPLETED')),
  job_exception VARCHAR(5000),
  id varchar(255) not null,
  primary key (id)
);