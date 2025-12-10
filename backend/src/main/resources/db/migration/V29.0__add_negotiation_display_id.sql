-- Add display_id column to negotiation table
ALTER TABLE negotiation
ADD COLUMN display_id VARCHAR(255);

-- Optional: If you want to populate the new column with existing data, you can use an UPDATE statement here
UPDATE negotiation 
SET display_id = id;

-- Create a sequence for generating unique display_id values
create sequence negotiation_display_id_seq
    start with 2000
    increment by 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;