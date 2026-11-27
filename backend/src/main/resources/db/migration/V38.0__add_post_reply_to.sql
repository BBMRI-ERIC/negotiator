ALTER TABLE post
    ADD COLUMN reply_to_id VARCHAR(255);

ALTER TABLE post
    ADD CONSTRAINT fk_post_on_reply_to FOREIGN KEY (reply_to_id) REFERENCES post (id) ON DELETE SET NULL;

CREATE INDEX idx_post_reply_to_id ON post (reply_to_id);
