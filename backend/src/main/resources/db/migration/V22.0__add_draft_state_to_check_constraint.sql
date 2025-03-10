BEGIN;
    ALTER TABLE negotiation DROP CONSTRAINT negotiation_current_state_check;

    ALTER TABLE negotiation
    ADD CONSTRAINT negotiation_current_state_check CHECK (((current_state)::text = ANY ((ARRAY['DRAFT'::character varying, 'SUBMITTED'::character varying, 'APPROVED'::character varying, 'DECLINED'::character varying, 'IN_PROGRESS'::character varying, 'PAUSED'::character varying, 'CONCLUDED'::character varying, 'ABANDONED'::character varying])::text[])));

    ALTER TABLE negotiation_lifecycle_record
    DROP CONSTRAINT negotiation_lifecycle_record_changed_to_check;

    ALTER TABLE negotiation_lifecycle_record
    ADD CONSTRAINT negotiation_lifecycle_record_changed_to_check CHECK (((changed_to)::text = ANY ((ARRAY['DRAFT'::character varying, 'SUBMITTED'::character varying, 'APPROVED'::character varying, 'DECLINED'::character varying, 'IN_PROGRESS'::character varying, 'PAUSED'::character varying, 'CONCLUDED'::character varying, 'ABANDONED'::character varying])::text[])));

    COMMIT;
END;