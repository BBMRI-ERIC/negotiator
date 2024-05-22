create TABLE discovery_service_synchronization_job (
    id character varying(255) NOT NULL,
    discovery_service_id bigint NOT NULL,
    creation_date timestamp(6) without time zone,
    modified_date timestamp(6) without time zone,
    status character varying(255),
    CONSTRAINT discovery_service_synchronization_job_status_check CHECK (((status)::text = ANY ((ARRAY['SUBMITTED'::character varying, 'PROCESSED'::character varying, 'IN_PROGRESS'::character varying, 'FAILED'::character varying, 'COMPLETED'::character varying])::text[])))
 );

alter table discovery_service_synchronization_job
    ADD CONSTRAINT discovery_service_synchronization_job_pkey PRIMARY KEY (id);

alter table discovery_service_synchronization_job
    ADD CONSTRAINT fkey_discovery_service_id FOREIGN KEY (discovery_service_id) REFERENCES discovery_service(id);