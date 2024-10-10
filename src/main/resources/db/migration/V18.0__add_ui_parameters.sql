create TABLE ui_parameter
(
    id       INT       NOT NULL,
    category VARCHAR(255) NOT NULL,
    name     VARCHAR(255) NOT NULL,
    type     VARCHAR(255) NOT NULL,
    value    VARCHAR(255) NOT NULL,
    CONSTRAINT pk_ui_parameter PRIMARY KEY (id),
    CONSTRAINT ui_parameter_type_check CHECK (((type)::text = ANY ((ARRAY['STRING'::character varying, 'INT'::character varying, 'BOOL'::character varying])::text[])))
);