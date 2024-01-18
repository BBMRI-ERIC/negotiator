ALTER TABLE public.access_criteria
    ADD COLUMN creation_date timestamp(6) without time zone,
    ADD COLUMN modified_date timestamp(6) without time zone,
    ADD COLUMN created_by bigint REFERENCES public.person(id),
    ADD COLUMN modified_by bigint REFERENCES public.person(id);

ALTER TABLE public.access_criteria_section
    ADD COLUMN creation_date timestamp(6) without time zone,
    ADD COLUMN modified_date timestamp(6) without time zone,
    ADD COLUMN created_by bigint REFERENCES public.person(id),
    ADD COLUMN modified_by bigint REFERENCES public.person(id);

ALTER TABLE public.access_criteria_section_link
    ADD COLUMN creation_date timestamp(6) without time zone,
    ADD COLUMN modified_date timestamp(6) without time zone,
    ADD COLUMN created_by bigint REFERENCES public.person(id),
    ADD COLUMN modified_by bigint REFERENCES public.person(id);

ALTER TABLE public.access_criteria_set
    ADD COLUMN creation_date timestamp(6) without time zone,
    ADD COLUMN modified_date timestamp(6) without time zone,
    ADD COLUMN created_by bigint REFERENCES public.person(id),
    ADD COLUMN modified_by bigint REFERENCES public.person(id);

ALTER TABLE public.data_source
    ADD COLUMN creation_date timestamp(6) without time zone,
    ADD COLUMN modified_date timestamp(6) without time zone,
    ADD COLUMN created_by bigint REFERENCES public.person(id),
    ADD COLUMN modified_by bigint REFERENCES public.person(id);

ALTER TABLE public.negotiation_lifecycle_record
    RENAME COLUMN recorded_at TO creation_date;

ALTER TABLE public.negotiation_lifecycle_record
    ALTER COLUMN creation_date TYPE timestamp(6) without time zone,
    ADD COLUMN modified_date timestamp(6) without time zone,
    ADD COLUMN created_by bigint REFERENCES public.person(id),
    ADD COLUMN modified_by bigint REFERENCES public.person(id);

ALTER TABLE public.notification
    ADD COLUMN modified_date timestamp(6) without time zone,
    ADD COLUMN created_by bigint REFERENCES public.person(id),
    ADD COLUMN modified_by bigint REFERENCES public.person(id);

ALTER TABLE public.notification_email
    RENAME COLUMN sent_at TO creation_date;

ALTER TABLE public.notification_email
    ADD COLUMN modified_date timestamp(6) without time zone,
    ADD COLUMN created_by bigint REFERENCES public.person(id),
    ADD COLUMN modified_by bigint REFERENCES public.person(id);

ALTER TABLE public.organization
    ADD COLUMN creation_date timestamp(6) without time zone,
    ADD COLUMN modified_date timestamp(6) without time zone,
    ADD COLUMN created_by bigint REFERENCES public.person(id),
    ADD COLUMN modified_by bigint REFERENCES public.person(id);

ALTER TABLE public.person
    ADD COLUMN creation_date timestamp(6) without time zone,
    ADD COLUMN modified_date timestamp(6) without time zone,
    ADD COLUMN created_by bigint REFERENCES public.person(id),
    ADD COLUMN modified_by bigint REFERENCES public.person(id);

ALTER TABLE public.person_negotiation_role
    ADD COLUMN creation_date timestamp(6) without time zone,
    ADD COLUMN modified_date timestamp(6) without time zone,
    ADD COLUMN created_by bigint REFERENCES public.person(id),
    ADD COLUMN modified_by bigint REFERENCES public.person(id);

ALTER TABLE public.person_project_role
    ADD COLUMN creation_date timestamp(6) without time zone,
    ADD COLUMN modified_date timestamp(6) without time zone,
    ADD COLUMN created_by bigint REFERENCES public.person(id),
    ADD COLUMN modified_by bigint REFERENCES public.person(id);