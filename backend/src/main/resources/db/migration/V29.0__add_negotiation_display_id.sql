-- Add display_id column to negotiation table
ALTER TABLE negotiation
ADD COLUMN display_id VARCHAR(255);

-- Create a sequence for generating unique display_id values
create sequence negotiation_display_id_seq
    start with 1
    increment by 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- TODO: Update existing negotiation records to have a display_id and adjust the sequence accordingly