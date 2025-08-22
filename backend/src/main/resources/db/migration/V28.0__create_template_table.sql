-- Create template table for storing templates in the database
-- This migration enables database-based template storage for both email notifications and PDF generation

CREATE TABLE template (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    content TEXT NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create index on name column for faster lookups
CREATE INDEX idx_template_name ON template(name);
