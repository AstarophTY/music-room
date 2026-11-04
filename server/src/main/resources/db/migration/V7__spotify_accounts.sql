-- Spotify account connected by each user. Tokens are encrypted (SecretBox) and refreshed by the server.
CREATE TABLE spotify_accounts (
    user_id         UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    spotify_user_id VARCHAR(128)  NOT NULL,
    display_name    VARCHAR(255),
    product         VARCHAR(32),
    access_token    VARCHAR(1024) NOT NULL,
    refresh_token   VARCHAR(1024) NOT NULL,
    expires_at      TIMESTAMPTZ   NOT NULL,
    scope           VARCHAR(512),
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT now()
);
