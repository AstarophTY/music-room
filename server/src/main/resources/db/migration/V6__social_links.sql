-- Usernames on social networks shown on the profile (one per network).
CREATE TABLE user_social_links (
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    network    VARCHAR(16) NOT NULL,
    handle     VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, network)
);
