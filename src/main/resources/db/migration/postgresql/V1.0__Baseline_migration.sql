create TABLE access_criteria (
    id bigint NOT NULL,
    description character varying(255) NOT NULL,
    label character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    type character varying(255) NOT NULL,
    access_criteria_section_id bigint
);

create sequence access_criteria_id_seq
    start with 1
    increment by 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

alter sequence access_criteria_id_seq OWNED BY access_criteria.id;

create TABLE access_criteria_section (
    id bigint NOT NULL,
    description character varying(255) NOT NULL,
    label character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    access_criteria_set_id bigint
);

create sequence access_criteria_section_id_seq
    start with 1
    increment by 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

alter sequence access_criteria_section_id_seq OWNED BY access_criteria_section.id;

create TABLE access_criteria_section_link (
    access_criteria_id bigint NOT NULL,
    access_criteria_section_id bigint NOT NULL,
    ordering integer NOT NULL,
    required boolean NOT NULL
);

create TABLE access_criteria_set (
    id bigint NOT NULL,
    name character varying(255)
);

create sequence access_criteria_set_id_seq
    start with 1
    increment by 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

alter sequence access_criteria_set_id_seq OWNED BY access_criteria_set.id;


create TABLE attachment (
    id character varying(255) NOT NULL,
    creation_date timestamp(6) without time zone,
    modified_date timestamp(6) without time zone,
    content_type character varying(255),
    name character varying(255),
    payload bytea,
    size bigint,
    created_by bigint,
    modified_by bigint,
    negotiation_id character varying(255),
    organization_id bigint
);


create TABLE authorities (
    id bigint NOT NULL,
    authority character varying(255),
    person_id bigint
);


create sequence authorities_id_seq
    start with 1
    increment by 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


alter sequence authorities_id_seq OWNED BY authorities.id;


create TABLE data_source (
    id bigint NOT NULL,
    api_password character varying(255) NOT NULL,
    api_type character varying(255) NOT NULL,
    api_url character varying(255) NOT NULL,
    api_username character varying(255) NOT NULL,
    description character varying(255),
    name character varying(255) NOT NULL,
    resource_biobank character varying(255) NOT NULL,
    resource_collection character varying(255) NOT NULL,
    resource_network character varying(255) NOT NULL,
    source_prefix character varying(255),
    sync_active boolean NOT NULL,
    url character varying(255) NOT NULL,
    CONSTRAINT data_source_api_type_check CHECK (((api_type)::text = 'MOLGENIS'::text))
);


create sequence data_source_id_seq
    start with 1
    increment by 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

alter sequence data_source_id_seq OWNED BY data_source.id;


create TABLE negotiation (
    id character varying(255) NOT NULL,
    creation_date timestamp(6) without time zone,
    modified_date timestamp(6) without time zone,
    current_state character varying(255),
    payload jsonb,
    posts_enabled boolean,
    created_by bigint,
    modified_by bigint,
    CONSTRAINT negotiation_current_state_check CHECK (((current_state)::text = ANY ((ARRAY['SUBMITTED'::character varying, 'APPROVED'::character varying, 'DECLINED'::character varying, 'IN_PROGRESS'::character varying, 'PAUSED'::character varying, 'CONCLUDED'::character varying, 'ABANDONED'::character varying])::text[])))
);


create TABLE negotiation_lifecycle_record (
    id bigint NOT NULL,
    changed_to character varying(255),
    recorded_at timestamp(6) with time zone,
    negotiation_id character varying(255),
    CONSTRAINT negotiation_lifecycle_record_changed_to_check CHECK (((changed_to)::text = ANY ((ARRAY['SUBMITTED'::character varying, 'APPROVED'::character varying, 'DECLINED'::character varying, 'IN_PROGRESS'::character varying, 'PAUSED'::character varying, 'CONCLUDED'::character varying, 'ABANDONED'::character varying])::text[])))
);


create sequence negotiation_lifecycle_record_id_seq
    start with 1
    increment by 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


alter sequence negotiation_lifecycle_record_id_seq OWNED BY negotiation_lifecycle_record.id;


create TABLE notification (
    id bigint NOT NULL,
    creation_date timestamp(6) without time zone,
    email_status character varying(255),
    message text,
    negotiation_id character varying(255),
    recipient_id bigint,
    CONSTRAINT notification_email_status_check CHECK (((email_status)::text = ANY ((ARRAY['EMAIL_SENT'::character varying, 'EMAIL_NOT_SENT'::character varying])::text[])))
);


create TABLE notification_email (
    id bigint NOT NULL,
    message text,
    sent_at timestamp(6) without time zone,
    was_successfully_sent boolean NOT NULL,
    person_id bigint
);


create sequence notification_email_id_seq
    start with 1
    increment by 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


alter sequence notification_email_id_seq OWNED BY notification_email.id;


create sequence notification_id_seq
    start with 10000
    increment by 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


create TABLE organization (
    id bigint NOT NULL,
    external_id character varying(255) NOT NULL,
    name character varying(255)
);


create TABLE person (
    id bigint NOT NULL,
    admin boolean DEFAULT false NOT NULL,
    email character varying(255) NOT NULL,
    is_service_account boolean DEFAULT false NOT NULL,
    name character varying(255) NOT NULL,
    organization character varying(255),
    password character varying(255),
    subject_id character varying(255) NOT NULL
);


create sequence person_id_seq
    start with 10000
    increment by 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


create TABLE person_negotiation_role (
    negotiation_id character varying(255) NOT NULL,
    person_id bigint NOT NULL,
    role_id bigint NOT NULL
);


create TABLE person_project_link (
    person_id bigint NOT NULL,
    project_id character varying(255) NOT NULL
);


create TABLE person_project_role (
    person_id bigint NOT NULL,
    project_id character varying(255) NOT NULL,
    role_id bigint
);


create TABLE post (
    id character varying(255) NOT NULL,
    creation_date timestamp(6) without time zone,
    modified_date timestamp(6) without time zone,
    status character varying(255),
    text text,
    type character varying(255),
    created_by bigint,
    modified_by bigint,
    request_id character varying(255),
    organization_id bigint,
    CONSTRAINT post_status_check CHECK (((status)::text = ANY ((ARRAY['CREATED'::character varying, 'READ'::character varying])::text[]))),
    CONSTRAINT post_type_check CHECK (((type)::text = ANY ((ARRAY['PRIVATE'::character varying, 'PUBLIC'::character varying])::text[])))
);


create TABLE project (
    id character varying(255) NOT NULL,
    creation_date timestamp(6) without time zone,
    modified_date timestamp(6) without time zone,
    payload jsonb NOT NULL,
    created_by bigint,
    modified_by bigint
);


create TABLE request (
    id character varying(255) NOT NULL,
    human_readable text NOT NULL,
    url text NOT NULL,
    data_source_id bigint NOT NULL,
    negotiation_id character varying(255)
);


create TABLE request_resources_link (
    request_id character varying(255) NOT NULL,
    resource_id bigint NOT NULL
);


create TABLE resource (
    id bigint NOT NULL,
    description character varying(5000),
    name character varying(255),
    source_id character varying(255) NOT NULL,
    access_criteria_set_id bigint,
    data_source_id bigint NOT NULL,
    organization_id bigint NOT NULL
);


create sequence resource_id_seq
    start with 10000
    increment by 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


create TABLE resource_representative_link (
    person_id bigint NOT NULL,
    resource_id bigint NOT NULL
);


create TABLE resource_state_per_negotiation (
    negotiation_id character varying(255) NOT NULL,
    current_state character varying(255),
    resource_id character varying(255) NOT NULL,
    CONSTRAINT resource_state_per_negotiation_current_state_check CHECK (((current_state)::text = ANY ((ARRAY['SUBMITTED'::character varying, 'REPRESENTATIVE_CONTACTED'::character varying, 'REPRESENTATIVE_UNREACHABLE'::character varying, 'RETURNED_FOR_RESUBMISSION'::character varying, 'CHECKING_AVAILABILITY'::character varying, 'RESOURCE_AVAILABLE'::character varying, 'RESOURCE_UNAVAILABLE_WILLING_TO_COLLECT'::character varying, 'RESOURCE_UNAVAILABLE'::character varying, 'ACCESS_CONDITIONS_INDICATED'::character varying, 'ACCESS_CONDITIONS_MET'::character varying, 'RESOURCE_NOT_MADE_AVAILABLE'::character varying, 'RESOURCE_MADE_AVAILABLE'::character varying])::text[])))
);


create TABLE role (
    id bigint NOT NULL,
    name character varying(255)
);


create sequence role_id_seq
    start with 1
    increment by 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


alter sequence role_id_seq OWNED BY role.id;


alter table ONLY access_criteria alter COLUMN id SET DEFAULT nextval('access_criteria_id_seq'::regclass);


alter table ONLY access_criteria_section alter COLUMN id SET DEFAULT nextval('access_criteria_section_id_seq'::regclass);


alter table ONLY access_criteria_set alter COLUMN id SET DEFAULT nextval('access_criteria_set_id_seq'::regclass);


alter table ONLY authorities alter COLUMN id SET DEFAULT nextval('authorities_id_seq'::regclass);


alter table ONLY data_source alter COLUMN id SET DEFAULT nextval('data_source_id_seq'::regclass);


alter table ONLY negotiation_lifecycle_record alter COLUMN id SET DEFAULT nextval('negotiation_lifecycle_record_id_seq'::regclass);


alter table ONLY notification_email alter COLUMN id SET DEFAULT nextval('notification_email_id_seq'::regclass);


alter table ONLY role alter COLUMN id SET DEFAULT nextval('role_id_seq'::regclass);


alter table ONLY access_criteria
    ADD CONSTRAINT access_criteria_pkey PRIMARY KEY (id);


alter table ONLY access_criteria_section_link
    ADD CONSTRAINT access_criteria_section_link_pkey PRIMARY KEY (access_criteria_id, access_criteria_section_id);


alter table ONLY access_criteria_section
    ADD CONSTRAINT access_criteria_section_pkey PRIMARY KEY (id);


alter table ONLY access_criteria_set
    ADD CONSTRAINT access_criteria_set_pkey PRIMARY KEY (id);


alter table ONLY attachment
    ADD CONSTRAINT attachment_pkey PRIMARY KEY (id);


alter table ONLY authorities
    ADD CONSTRAINT authorities_pkey PRIMARY KEY (id);


alter table ONLY data_source
    ADD CONSTRAINT data_source_pkey PRIMARY KEY (id);


alter table ONLY negotiation_lifecycle_record
    ADD CONSTRAINT negotiation_lifecycle_record_pkey PRIMARY KEY (id);


alter table ONLY negotiation
    ADD CONSTRAINT negotiation_pkey PRIMARY KEY (id);


alter table ONLY notification_email
    ADD CONSTRAINT notification_email_pkey PRIMARY KEY (id);


alter table ONLY notification
    ADD CONSTRAINT notification_pkey PRIMARY KEY (id);


alter table ONLY organization
    ADD CONSTRAINT organization_pkey PRIMARY KEY (id);


alter table ONLY person_negotiation_role
    ADD CONSTRAINT person_negotiation_role_pkey PRIMARY KEY (negotiation_id, person_id, role_id);


alter table ONLY person
    ADD CONSTRAINT person_pkey PRIMARY KEY (id);


alter table ONLY person_project_link
    ADD CONSTRAINT person_project_link_pkey PRIMARY KEY (person_id, project_id);


alter table ONLY person_project_role
    ADD CONSTRAINT person_project_role_pkey PRIMARY KEY (person_id, project_id);


alter table ONLY post
    ADD CONSTRAINT post_pkey PRIMARY KEY (id);


alter table ONLY project
    ADD CONSTRAINT project_pkey PRIMARY KEY (id);


alter table ONLY request
    ADD CONSTRAINT request_pkey PRIMARY KEY (id);


alter table ONLY request_resources_link
    ADD CONSTRAINT request_resources_link_pkey PRIMARY KEY (request_id, resource_id);


alter table ONLY resource
    ADD CONSTRAINT resource_pkey PRIMARY KEY (id);


alter table ONLY resource_representative_link
    ADD CONSTRAINT resource_representative_link_pkey PRIMARY KEY (person_id, resource_id);


alter table ONLY resource_state_per_negotiation
    ADD CONSTRAINT resource_state_per_negotiation_pkey PRIMARY KEY (negotiation_id, resource_id);


alter table ONLY role
    ADD CONSTRAINT role_pkey PRIMARY KEY (id);


alter table ONLY organization
    ADD CONSTRAINT uk_anbatb82bwhrx4gagnflykthh UNIQUE (external_id);


alter table ONLY person_project_role
    ADD CONSTRAINT uk_bxuryor3aidjr67si0gcpydq2 UNIQUE (role_id);


alter table ONLY data_source
    ADD CONSTRAINT uk_byao32rsksrj7vkas653sl224 UNIQUE (url);


alter table ONLY person
    ADD CONSTRAINT uk_qd3ofyfsy04d9j3qor20nm7ly UNIQUE (subject_id);


alter table ONLY request
    ADD CONSTRAINT fk1dblesuyfmaxqj8vvs43ttiut FOREIGN KEY (data_source_id) REFERENCES data_source(id);


alter table ONLY resource
    ADD CONSTRAINT fk1xa946oabsglyyf25u09d0nuu FOREIGN KEY (access_criteria_set_id) REFERENCES access_criteria_set(id);


alter table ONLY resource
    ADD CONSTRAINT fk2c4lb6ow7camgvn82itk6b68j FOREIGN KEY (data_source_id) REFERENCES data_source(id);


alter table ONLY person_project_link
    ADD CONSTRAINT fk381qbe3krq75p31d7u25siedv FOREIGN KEY (person_id) REFERENCES person(id);


alter table ONLY post
    ADD CONSTRAINT fk3bl88xxkqs4cgun3vk9ur3shu FOREIGN KEY (request_id) REFERENCES negotiation(id);


alter table ONLY negotiation
    ADD CONSTRAINT fk62916aq48ihicemrlbwk8cj10 FOREIGN KEY (created_by) REFERENCES person(id);


alter table ONLY person_project_link
    ADD CONSTRAINT fk77bria9ttghyi5jhgc9k88pk FOREIGN KEY (project_id) REFERENCES project(id);


alter table ONLY notification
    ADD CONSTRAINT fk7wywiqhssp9wjk9ijg0lms4hv FOREIGN KEY (recipient_id) REFERENCES person(id);


alter table ONLY post
    ADD CONSTRAINT fk8k515auw0oorgyqymc1tnwat1 FOREIGN KEY (modified_by) REFERENCES person(id);


alter table ONLY attachment
    ADD CONSTRAINT fk963a0q97mrss1fxy1f51vfm0b FOREIGN KEY (created_by) REFERENCES person(id);


alter table ONLY person_negotiation_role
    ADD CONSTRAINT fka5uex2honkxkp7a4f7lxrgikn FOREIGN KEY (role_id) REFERENCES role(id);


alter table ONLY person_project_role
    ADD CONSTRAINT fkbdqx0q8bawb93ktue0f772rm1 FOREIGN KEY (project_id) REFERENCES project(id);


alter table ONLY request_resources_link
    ADD CONSTRAINT fkblq0pwafdnlwdk4l3me652hfw FOREIGN KEY (resource_id) REFERENCES resource(id);


alter table ONLY resource_state_per_negotiation
    ADD CONSTRAINT fkc738qoch93u1ybjmiydhf2h58 FOREIGN KEY (negotiation_id) REFERENCES negotiation(id);


alter table ONLY access_criteria_section_link
    ADD CONSTRAINT fkcgj2extn02c91q2ld1xvp54di FOREIGN KEY (access_criteria_section_id) REFERENCES access_criteria_section(id);


alter table ONLY resource_representative_link
    ADD CONSTRAINT fkey1ddlsd86tu78rxjnqaaapwa FOREIGN KEY (person_id) REFERENCES person(id);


alter table ONLY post
    ADD CONSTRAINT fkfs4ns7nd5pb3k72nbt8g2xq3d FOREIGN KEY (created_by) REFERENCES person(id);


alter table ONLY attachment
    ADD CONSTRAINT fkhgi1r4d3ylfiqalc4qlru8yhu FOREIGN KEY (negotiation_id) REFERENCES negotiation(id);


alter table ONLY negotiation_lifecycle_record
    ADD CONSTRAINT fkhmbnhi74t9tve5ghbf7hb7r0p FOREIGN KEY (negotiation_id) REFERENCES negotiation(id);


alter table ONLY access_criteria_section_link
    ADD CONSTRAINT fkhwqk0a4nxt3p18e97l4llg4bs FOREIGN KEY (access_criteria_id) REFERENCES access_criteria(id);


alter table ONLY access_criteria_section
    ADD CONSTRAINT fkijwhm9gjj8qdo5eu09iibxdbo FOREIGN KEY (access_criteria_set_id) REFERENCES access_criteria_set(id);


alter table ONLY notification_email
    ADD CONSTRAINT fkjsocvwlnpmpp0ftmo5nm8nmm7 FOREIGN KEY (person_id) REFERENCES person(id);


alter table ONLY project
    ADD CONSTRAINT fkjyr9p0nibm4qmaddopb52ksa0 FOREIGN KEY (modified_by) REFERENCES person(id);


alter table ONLY request_resources_link
    ADD CONSTRAINT fkl8i1lbs2jfsl7or12i0hi9qkb FOREIGN KEY (request_id) REFERENCES request(id);


alter table ONLY person_project_role
    ADD CONSTRAINT fkmnc7g89mav2nlhshfnm4ta4dn FOREIGN KEY (role_id) REFERENCES role(id);


alter table ONLY authorities
    ADD CONSTRAINT fkn55wpgxf39f3kda1tw6ia2mgn FOREIGN KEY (person_id) REFERENCES person(id);


alter table ONLY negotiation
    ADD CONSTRAINT fknb8a0248715b96boouf3973r7 FOREIGN KEY (modified_by) REFERENCES person(id);


alter table ONLY resource
    ADD CONSTRAINT fknxy1sei1miecaw4aju99ce32u FOREIGN KEY (organization_id) REFERENCES organization(id);


alter table ONLY post
    ADD CONSTRAINT fkowe48u8aic9c83l1edrgmft53 FOREIGN KEY (organization_id) REFERENCES organization(id);


alter table ONLY person_negotiation_role
    ADD CONSTRAINT fkp514tee0gwjcd11yycx40j17m FOREIGN KEY (negotiation_id) REFERENCES negotiation(id);


alter table ONLY person_negotiation_role
    ADD CONSTRAINT fkp6fcqx8iyy2wcrykniqcic2ge FOREIGN KEY (person_id) REFERENCES person(id);


alter table ONLY resource_representative_link
    ADD CONSTRAINT fkpda0l0e7a0kk7aj8d7dd8blr2 FOREIGN KEY (resource_id) REFERENCES resource(id);


alter table ONLY notification
    ADD CONSTRAINT fkq1tn3w9gjmypghc58magu8i2t FOREIGN KEY (negotiation_id) REFERENCES negotiation(id);


alter table ONLY person_project_role
    ADD CONSTRAINT fkqt0afjgr9fxqyjxeox9xxd4c FOREIGN KEY (person_id) REFERENCES person(id);


alter table ONLY project
    ADD CONSTRAINT fkqwy6ntg495nxuiyc6prb6o8h0 FOREIGN KEY (created_by) REFERENCES person(id);


alter table ONLY request
    ADD CONSTRAINT fkr3i632eg511015o18i0s6x014 FOREIGN KEY (negotiation_id) REFERENCES negotiation(id);


alter table ONLY attachment
    ADD CONSTRAINT fksbxoy0aohg40gdhkg2ks7ak9y FOREIGN KEY (modified_by) REFERENCES person(id);


alter table ONLY access_criteria
    ADD CONSTRAINT fkt4trpxmxhenaxg0i4duggiocr FOREIGN KEY (access_criteria_section_id) REFERENCES access_criteria_section(id);


alter table ONLY attachment
    ADD CONSTRAINT fktqlmqtq11hl9mhchr4sna6rrh FOREIGN KEY (organization_id) REFERENCES organization(id);
