CREATE TABLE media_assets (
      id UUID PRIMARY KEY,
      storage_key VARCHAR(255) NOT NULL UNIQUE,
      original_filename VARCHAR(255) NOT NULL,
      content_type VARCHAR(100) NOT NULL,
      size_bytes BIGINT NOT NULL CHECK (size_bytes > 0),
      uploaded_by BIGINT NOT NULL REFERENCES users (id),
      created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX ix_media_assets_uploaded_by ON media_assets (uploaded_by);
