--
-- PostgreSQL database dump
--

-- Dumped from database version 14.10 (Debian 14.10-1.pgdg120+1)
-- Dumped by pg_dump version 14.10 (Debian 14.10-1.pgdg120+1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: access_criteria; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.access_criteria (
    id bigint NOT NULL,
    description character varying(255) NOT NULL,
    label character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    type character varying(255) NOT NULL,
    access_criteria_section_id bigint
);


--
-- Name: access_criteria_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.access_criteria_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: access_criteria_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.access_criteria_id_seq OWNED BY public.access_criteria.id;


--
-- Name: access_criteria_section; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.access_criteria_section (
    id bigint NOT NULL,
    description character varying(255) NOT NULL,
    label character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    access_criteria_set_id bigint
);


--
-- Name: access_criteria_section_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.access_criteria_section_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: access_criteria_section_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.access_criteria_section_id_seq OWNED BY public.access_criteria_section.id;


--
-- Name: access_criteria_section_link; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.access_criteria_section_link (
    access_criteria_id bigint NOT NULL,
    access_criteria_section_id bigint NOT NULL,
    ordering integer NOT NULL,
    required boolean NOT NULL
);


--
-- Name: access_criteria_set; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.access_criteria_set (
    id bigint NOT NULL,
    name character varying(255)
);


--
-- Name: access_criteria_set_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.access_criteria_set_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: access_criteria_set_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.access_criteria_set_id_seq OWNED BY public.access_criteria_set.id;


--
-- Name: attachment; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.attachment (
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


--
-- Name: authorities; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.authorities (
    id bigint NOT NULL,
    authority character varying(255),
    person_id bigint
);


--
-- Name: authorities_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.authorities_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: authorities_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.authorities_id_seq OWNED BY public.authorities.id;


--
-- Name: data_source; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.data_source (
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


--
-- Name: data_source_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.data_source_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: data_source_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.data_source_id_seq OWNED BY public.data_source.id;


--
-- Name: negotiation; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.negotiation (
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


--
-- Name: negotiation_lifecycle_record; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.negotiation_lifecycle_record (
    id bigint NOT NULL,
    changed_to character varying(255),
    recorded_at timestamp(6) with time zone,
    negotiation_id character varying(255),
    CONSTRAINT negotiation_lifecycle_record_changed_to_check CHECK (((changed_to)::text = ANY ((ARRAY['SUBMITTED'::character varying, 'APPROVED'::character varying, 'DECLINED'::character varying, 'IN_PROGRESS'::character varying, 'PAUSED'::character varying, 'CONCLUDED'::character varying, 'ABANDONED'::character varying])::text[])))
);


--
-- Name: negotiation_lifecycle_record_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.negotiation_lifecycle_record_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: negotiation_lifecycle_record_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.negotiation_lifecycle_record_id_seq OWNED BY public.negotiation_lifecycle_record.id;


--
-- Name: notification; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.notification (
    id bigint NOT NULL,
    creation_date timestamp(6) without time zone,
    email_status character varying(255),
    message text,
    negotiation_id character varying(255),
    recipient_id bigint,
    CONSTRAINT notification_email_status_check CHECK (((email_status)::text = ANY ((ARRAY['EMAIL_SENT'::character varying, 'EMAIL_NOT_SENT'::character varying])::text[])))
);


--
-- Name: notification_email; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.notification_email (
    id bigint NOT NULL,
    message text,
    sent_at timestamp(6) without time zone,
    was_successfully_sent boolean NOT NULL,
    person_id bigint
);


--
-- Name: notification_email_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.notification_email_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: notification_email_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.notification_email_id_seq OWNED BY public.notification_email.id;


--
-- Name: notification_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.notification_id_seq
    START WITH 10000
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: organization; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.organization (
    id bigint NOT NULL,
    external_id character varying(255) NOT NULL,
    name character varying(255)
);


--
-- Name: person; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.person (
    id bigint NOT NULL,
    admin boolean DEFAULT false NOT NULL,
    email character varying(255) NOT NULL,
    is_service_account boolean DEFAULT false NOT NULL,
    name character varying(255) NOT NULL,
    organization character varying(255),
    password character varying(255),
    subject_id character varying(255) NOT NULL
);


--
-- Name: person_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.person_id_seq
    START WITH 300
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: person_negotiation_role; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.person_negotiation_role (
    negotiation_id character varying(255) NOT NULL,
    person_id bigint NOT NULL,
    role_id bigint NOT NULL
);


--
-- Name: person_project_link; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.person_project_link (
    person_id bigint NOT NULL,
    project_id character varying(255) NOT NULL
);


--
-- Name: person_project_role; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.person_project_role (
    person_id bigint NOT NULL,
    project_id character varying(255) NOT NULL,
    role_id bigint
);


--
-- Name: post; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.post (
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


--
-- Name: project; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.project (
    id character varying(255) NOT NULL,
    creation_date timestamp(6) without time zone,
    modified_date timestamp(6) without time zone,
    payload jsonb NOT NULL,
    created_by bigint,
    modified_by bigint
);


--
-- Name: request; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.request (
    id character varying(255) NOT NULL,
    human_readable text NOT NULL,
    url text NOT NULL,
    data_source_id bigint NOT NULL,
    negotiation_id character varying(255)
);


--
-- Name: request_resources_link; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.request_resources_link (
    request_id character varying(255) NOT NULL,
    resource_id bigint NOT NULL
);


--
-- Name: resource; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.resource (
    id bigint NOT NULL,
    description character varying(5000),
    name character varying(255),
    source_id character varying(255) NOT NULL,
    access_criteria_set_id bigint,
    data_source_id bigint NOT NULL,
    organization_id bigint NOT NULL
);


--
-- Name: resource_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.resource_id_seq
    START WITH 10000
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: resource_representative_link; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.resource_representative_link (
    person_id bigint NOT NULL,
    resource_id bigint NOT NULL
);


--
-- Name: resource_state_per_negotiation; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.resource_state_per_negotiation (
    negotiation_id character varying(255) NOT NULL,
    current_state character varying(255),
    resource_id character varying(255) NOT NULL,
    CONSTRAINT resource_state_per_negotiation_current_state_check CHECK (((current_state)::text = ANY ((ARRAY['SUBMITTED'::character varying, 'REPRESENTATIVE_CONTACTED'::character varying, 'REPRESENTATIVE_UNREACHABLE'::character varying, 'RETURNED_FOR_RESUBMISSION'::character varying, 'CHECKING_AVAILABILITY'::character varying, 'RESOURCE_AVAILABLE'::character varying, 'RESOURCE_UNAVAILABLE_WILLING_TO_COLLECT'::character varying, 'RESOURCE_UNAVAILABLE'::character varying, 'ACCESS_CONDITIONS_INDICATED'::character varying, 'ACCESS_CONDITIONS_MET'::character varying, 'RESOURCE_NOT_MADE_AVAILABLE'::character varying, 'RESOURCE_MADE_AVAILABLE'::character varying])::text[])))
);


--
-- Name: role; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.role (
    id bigint NOT NULL,
    name character varying(255)
);


--
-- Name: role_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.role_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: role_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.role_id_seq OWNED BY public.role.id;


--
-- Name: access_criteria id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.access_criteria ALTER COLUMN id SET DEFAULT nextval('public.access_criteria_id_seq'::regclass);


--
-- Name: access_criteria_section id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.access_criteria_section ALTER COLUMN id SET DEFAULT nextval('public.access_criteria_section_id_seq'::regclass);


--
-- Name: access_criteria_set id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.access_criteria_set ALTER COLUMN id SET DEFAULT nextval('public.access_criteria_set_id_seq'::regclass);


--
-- Name: authorities id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.authorities ALTER COLUMN id SET DEFAULT nextval('public.authorities_id_seq'::regclass);


--
-- Name: data_source id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.data_source ALTER COLUMN id SET DEFAULT nextval('public.data_source_id_seq'::regclass);


--
-- Name: negotiation_lifecycle_record id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.negotiation_lifecycle_record ALTER COLUMN id SET DEFAULT nextval('public.negotiation_lifecycle_record_id_seq'::regclass);


--
-- Name: notification_email id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notification_email ALTER COLUMN id SET DEFAULT nextval('public.notification_email_id_seq'::regclass);


--
-- Name: role id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.role ALTER COLUMN id SET DEFAULT nextval('public.role_id_seq'::regclass);


--
-- Name: access_criteria access_criteria_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.access_criteria
    ADD CONSTRAINT access_criteria_pkey PRIMARY KEY (id);


--
-- Name: access_criteria_section_link access_criteria_section_link_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.access_criteria_section_link
    ADD CONSTRAINT access_criteria_section_link_pkey PRIMARY KEY (access_criteria_id, access_criteria_section_id);


--
-- Name: access_criteria_section access_criteria_section_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.access_criteria_section
    ADD CONSTRAINT access_criteria_section_pkey PRIMARY KEY (id);


--
-- Name: access_criteria_set access_criteria_set_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.access_criteria_set
    ADD CONSTRAINT access_criteria_set_pkey PRIMARY KEY (id);


--
-- Name: attachment attachment_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.attachment
    ADD CONSTRAINT attachment_pkey PRIMARY KEY (id);


--
-- Name: authorities authorities_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.authorities
    ADD CONSTRAINT authorities_pkey PRIMARY KEY (id);


--
-- Name: data_source data_source_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.data_source
    ADD CONSTRAINT data_source_pkey PRIMARY KEY (id);


--
-- Name: negotiation_lifecycle_record negotiation_lifecycle_record_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.negotiation_lifecycle_record
    ADD CONSTRAINT negotiation_lifecycle_record_pkey PRIMARY KEY (id);


--
-- Name: negotiation negotiation_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.negotiation
    ADD CONSTRAINT negotiation_pkey PRIMARY KEY (id);


--
-- Name: notification_email notification_email_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notification_email
    ADD CONSTRAINT notification_email_pkey PRIMARY KEY (id);


--
-- Name: notification notification_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notification
    ADD CONSTRAINT notification_pkey PRIMARY KEY (id);


--
-- Name: organization organization_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.organization
    ADD CONSTRAINT organization_pkey PRIMARY KEY (id);


--
-- Name: person_negotiation_role person_negotiation_role_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.person_negotiation_role
    ADD CONSTRAINT person_negotiation_role_pkey PRIMARY KEY (negotiation_id, person_id, role_id);


--
-- Name: person person_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.person
    ADD CONSTRAINT person_pkey PRIMARY KEY (id);


--
-- Name: person_project_link person_project_link_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.person_project_link
    ADD CONSTRAINT person_project_link_pkey PRIMARY KEY (person_id, project_id);


--
-- Name: person_project_role person_project_role_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.person_project_role
    ADD CONSTRAINT person_project_role_pkey PRIMARY KEY (person_id, project_id);


--
-- Name: post post_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.post
    ADD CONSTRAINT post_pkey PRIMARY KEY (id);


--
-- Name: project project_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project
    ADD CONSTRAINT project_pkey PRIMARY KEY (id);


--
-- Name: request request_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.request
    ADD CONSTRAINT request_pkey PRIMARY KEY (id);


--
-- Name: request_resources_link request_resources_link_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.request_resources_link
    ADD CONSTRAINT request_resources_link_pkey PRIMARY KEY (request_id, resource_id);


--
-- Name: resource resource_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.resource
    ADD CONSTRAINT resource_pkey PRIMARY KEY (id);


--
-- Name: resource_representative_link resource_representative_link_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.resource_representative_link
    ADD CONSTRAINT resource_representative_link_pkey PRIMARY KEY (person_id, resource_id);


--
-- Name: resource_state_per_negotiation resource_state_per_negotiation_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.resource_state_per_negotiation
    ADD CONSTRAINT resource_state_per_negotiation_pkey PRIMARY KEY (negotiation_id, resource_id);


--
-- Name: role role_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.role
    ADD CONSTRAINT role_pkey PRIMARY KEY (id);


--
-- Name: organization uk_anbatb82bwhrx4gagnflykthh; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.organization
    ADD CONSTRAINT uk_anbatb82bwhrx4gagnflykthh UNIQUE (external_id);


--
-- Name: person_project_role uk_bxuryor3aidjr67si0gcpydq2; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.person_project_role
    ADD CONSTRAINT uk_bxuryor3aidjr67si0gcpydq2 UNIQUE (role_id);


--
-- Name: data_source uk_byao32rsksrj7vkas653sl224; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.data_source
    ADD CONSTRAINT uk_byao32rsksrj7vkas653sl224 UNIQUE (url);


--
-- Name: person uk_qd3ofyfsy04d9j3qor20nm7ly; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.person
    ADD CONSTRAINT uk_qd3ofyfsy04d9j3qor20nm7ly UNIQUE (subject_id);


--
-- Name: request fk1dblesuyfmaxqj8vvs43ttiut; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.request
    ADD CONSTRAINT fk1dblesuyfmaxqj8vvs43ttiut FOREIGN KEY (data_source_id) REFERENCES public.data_source(id);


--
-- Name: resource fk1xa946oabsglyyf25u09d0nuu; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.resource
    ADD CONSTRAINT fk1xa946oabsglyyf25u09d0nuu FOREIGN KEY (access_criteria_set_id) REFERENCES public.access_criteria_set(id);


--
-- Name: resource fk2c4lb6ow7camgvn82itk6b68j; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.resource
    ADD CONSTRAINT fk2c4lb6ow7camgvn82itk6b68j FOREIGN KEY (data_source_id) REFERENCES public.data_source(id);


--
-- Name: person_project_link fk381qbe3krq75p31d7u25siedv; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.person_project_link
    ADD CONSTRAINT fk381qbe3krq75p31d7u25siedv FOREIGN KEY (person_id) REFERENCES public.person(id);


--
-- Name: post fk3bl88xxkqs4cgun3vk9ur3shu; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.post
    ADD CONSTRAINT fk3bl88xxkqs4cgun3vk9ur3shu FOREIGN KEY (request_id) REFERENCES public.negotiation(id);


--
-- Name: negotiation fk62916aq48ihicemrlbwk8cj10; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.negotiation
    ADD CONSTRAINT fk62916aq48ihicemrlbwk8cj10 FOREIGN KEY (created_by) REFERENCES public.person(id);


--
-- Name: person_project_link fk77bria9ttghyi5jhgc9k88pk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.person_project_link
    ADD CONSTRAINT fk77bria9ttghyi5jhgc9k88pk FOREIGN KEY (project_id) REFERENCES public.project(id);


--
-- Name: notification fk7wywiqhssp9wjk9ijg0lms4hv; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notification
    ADD CONSTRAINT fk7wywiqhssp9wjk9ijg0lms4hv FOREIGN KEY (recipient_id) REFERENCES public.person(id);


--
-- Name: post fk8k515auw0oorgyqymc1tnwat1; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.post
    ADD CONSTRAINT fk8k515auw0oorgyqymc1tnwat1 FOREIGN KEY (modified_by) REFERENCES public.person(id);


--
-- Name: attachment fk963a0q97mrss1fxy1f51vfm0b; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.attachment
    ADD CONSTRAINT fk963a0q97mrss1fxy1f51vfm0b FOREIGN KEY (created_by) REFERENCES public.person(id);


--
-- Name: person_negotiation_role fka5uex2honkxkp7a4f7lxrgikn; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.person_negotiation_role
    ADD CONSTRAINT fka5uex2honkxkp7a4f7lxrgikn FOREIGN KEY (role_id) REFERENCES public.role(id);


--
-- Name: person_project_role fkbdqx0q8bawb93ktue0f772rm1; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.person_project_role
    ADD CONSTRAINT fkbdqx0q8bawb93ktue0f772rm1 FOREIGN KEY (project_id) REFERENCES public.project(id);


--
-- Name: request_resources_link fkblq0pwafdnlwdk4l3me652hfw; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.request_resources_link
    ADD CONSTRAINT fkblq0pwafdnlwdk4l3me652hfw FOREIGN KEY (resource_id) REFERENCES public.resource(id);


--
-- Name: resource_state_per_negotiation fkc738qoch93u1ybjmiydhf2h58; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.resource_state_per_negotiation
    ADD CONSTRAINT fkc738qoch93u1ybjmiydhf2h58 FOREIGN KEY (negotiation_id) REFERENCES public.negotiation(id);


--
-- Name: access_criteria_section_link fkcgj2extn02c91q2ld1xvp54di; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.access_criteria_section_link
    ADD CONSTRAINT fkcgj2extn02c91q2ld1xvp54di FOREIGN KEY (access_criteria_section_id) REFERENCES public.access_criteria_section(id);


--
-- Name: resource_representative_link fkey1ddlsd86tu78rxjnqaaapwa; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.resource_representative_link
    ADD CONSTRAINT fkey1ddlsd86tu78rxjnqaaapwa FOREIGN KEY (person_id) REFERENCES public.person(id);


--
-- Name: post fkfs4ns7nd5pb3k72nbt8g2xq3d; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.post
    ADD CONSTRAINT fkfs4ns7nd5pb3k72nbt8g2xq3d FOREIGN KEY (created_by) REFERENCES public.person(id);


--
-- Name: attachment fkhgi1r4d3ylfiqalc4qlru8yhu; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.attachment
    ADD CONSTRAINT fkhgi1r4d3ylfiqalc4qlru8yhu FOREIGN KEY (negotiation_id) REFERENCES public.negotiation(id);


--
-- Name: negotiation_lifecycle_record fkhmbnhi74t9tve5ghbf7hb7r0p; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.negotiation_lifecycle_record
    ADD CONSTRAINT fkhmbnhi74t9tve5ghbf7hb7r0p FOREIGN KEY (negotiation_id) REFERENCES public.negotiation(id);


--
-- Name: access_criteria_section_link fkhwqk0a4nxt3p18e97l4llg4bs; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.access_criteria_section_link
    ADD CONSTRAINT fkhwqk0a4nxt3p18e97l4llg4bs FOREIGN KEY (access_criteria_id) REFERENCES public.access_criteria(id);


--
-- Name: access_criteria_section fkijwhm9gjj8qdo5eu09iibxdbo; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.access_criteria_section
    ADD CONSTRAINT fkijwhm9gjj8qdo5eu09iibxdbo FOREIGN KEY (access_criteria_set_id) REFERENCES public.access_criteria_set(id);


--
-- Name: notification_email fkjsocvwlnpmpp0ftmo5nm8nmm7; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notification_email
    ADD CONSTRAINT fkjsocvwlnpmpp0ftmo5nm8nmm7 FOREIGN KEY (person_id) REFERENCES public.person(id);


--
-- Name: project fkjyr9p0nibm4qmaddopb52ksa0; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project
    ADD CONSTRAINT fkjyr9p0nibm4qmaddopb52ksa0 FOREIGN KEY (modified_by) REFERENCES public.person(id);


--
-- Name: request_resources_link fkl8i1lbs2jfsl7or12i0hi9qkb; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.request_resources_link
    ADD CONSTRAINT fkl8i1lbs2jfsl7or12i0hi9qkb FOREIGN KEY (request_id) REFERENCES public.request(id);


--
-- Name: person_project_role fkmnc7g89mav2nlhshfnm4ta4dn; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.person_project_role
    ADD CONSTRAINT fkmnc7g89mav2nlhshfnm4ta4dn FOREIGN KEY (role_id) REFERENCES public.role(id);


--
-- Name: authorities fkn55wpgxf39f3kda1tw6ia2mgn; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.authorities
    ADD CONSTRAINT fkn55wpgxf39f3kda1tw6ia2mgn FOREIGN KEY (person_id) REFERENCES public.person(id);


--
-- Name: negotiation fknb8a0248715b96boouf3973r7; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.negotiation
    ADD CONSTRAINT fknb8a0248715b96boouf3973r7 FOREIGN KEY (modified_by) REFERENCES public.person(id);


--
-- Name: resource fknxy1sei1miecaw4aju99ce32u; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.resource
    ADD CONSTRAINT fknxy1sei1miecaw4aju99ce32u FOREIGN KEY (organization_id) REFERENCES public.organization(id);


--
-- Name: post fkowe48u8aic9c83l1edrgmft53; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.post
    ADD CONSTRAINT fkowe48u8aic9c83l1edrgmft53 FOREIGN KEY (organization_id) REFERENCES public.organization(id);


--
-- Name: person_negotiation_role fkp514tee0gwjcd11yycx40j17m; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.person_negotiation_role
    ADD CONSTRAINT fkp514tee0gwjcd11yycx40j17m FOREIGN KEY (negotiation_id) REFERENCES public.negotiation(id);


--
-- Name: person_negotiation_role fkp6fcqx8iyy2wcrykniqcic2ge; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.person_negotiation_role
    ADD CONSTRAINT fkp6fcqx8iyy2wcrykniqcic2ge FOREIGN KEY (person_id) REFERENCES public.person(id);


--
-- Name: resource_representative_link fkpda0l0e7a0kk7aj8d7dd8blr2; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.resource_representative_link
    ADD CONSTRAINT fkpda0l0e7a0kk7aj8d7dd8blr2 FOREIGN KEY (resource_id) REFERENCES public.resource(id);


--
-- Name: notification fkq1tn3w9gjmypghc58magu8i2t; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notification
    ADD CONSTRAINT fkq1tn3w9gjmypghc58magu8i2t FOREIGN KEY (negotiation_id) REFERENCES public.negotiation(id);


--
-- Name: person_project_role fkqt0afjgr9fxqyjxeox9xxd4c; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.person_project_role
    ADD CONSTRAINT fkqt0afjgr9fxqyjxeox9xxd4c FOREIGN KEY (person_id) REFERENCES public.person(id);


--
-- Name: project fkqwy6ntg495nxuiyc6prb6o8h0; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project
    ADD CONSTRAINT fkqwy6ntg495nxuiyc6prb6o8h0 FOREIGN KEY (created_by) REFERENCES public.person(id);


--
-- Name: request fkr3i632eg511015o18i0s6x014; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.request
    ADD CONSTRAINT fkr3i632eg511015o18i0s6x014 FOREIGN KEY (negotiation_id) REFERENCES public.negotiation(id);


--
-- Name: attachment fksbxoy0aohg40gdhkg2ks7ak9y; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.attachment
    ADD CONSTRAINT fksbxoy0aohg40gdhkg2ks7ak9y FOREIGN KEY (modified_by) REFERENCES public.person(id);


--
-- Name: access_criteria fkt4trpxmxhenaxg0i4duggiocr; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.access_criteria
    ADD CONSTRAINT fkt4trpxmxhenaxg0i4duggiocr FOREIGN KEY (access_criteria_section_id) REFERENCES public.access_criteria_section(id);


--
-- Name: attachment fktqlmqtq11hl9mhchr4sna6rrh; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.attachment
    ADD CONSTRAINT fktqlmqtq11hl9mhchr4sna6rrh FOREIGN KEY (organization_id) REFERENCES public.organization(id);


--
-- PostgreSQL database dump complete
--

