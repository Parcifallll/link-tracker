CREATE TABLE IF NOT EXISTS links
(
    id              BIGSERIAL PRIMARY KEY,
    url             TEXT        NOT NULL UNIQUE,
    last_checked_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_links_url ON links (url);
