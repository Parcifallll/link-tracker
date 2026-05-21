CREATE TABLE IF NOT EXISTS subscriptions
(
    chat_id BIGINT NOT NULL REFERENCES chats (chat_id) ON DELETE CASCADE,
    link_id BIGINT NOT NULL REFERENCES links (id) ON DELETE CASCADE,
    tags    TEXT[] DEFAULT '{}',
    filters TEXT[] DEFAULT '{}',
    PRIMARY KEY (chat_id, link_id)
);

CREATE INDEX IF NOT EXISTS idx_subscriptions_chat_id ON subscriptions (chat_id);
CREATE INDEX IF NOT EXISTS idx_subscriptions_link_id ON subscriptions (link_id);
