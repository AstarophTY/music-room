-- Music Room Premium, paid with Stripe: one row per user who started paying. status: the Stripe subscription status
-- ('active' and 'trialing' give Premium), 'none' before the first subscription.
CREATE TABLE subscriptions (
    user_id                UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    stripe_customer_id     VARCHAR(64) NOT NULL UNIQUE,
    stripe_subscription_id VARCHAR(64),
    status                 VARCHAR(32) NOT NULL DEFAULT 'none',
    current_period_end     TIMESTAMPTZ,
    cancel_at_period_end   BOOLEAN     NOT NULL DEFAULT false,
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Songs uploaded by Premium users (MP3), played by the app. Their audio is kept apart so listing them stays light.
CREATE TABLE hosted_tracks (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id    UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    title       VARCHAR(100) NOT NULL,
    artist      VARCHAR(100) NOT NULL,
    duration_ms BIGINT       NOT NULL,
    size_bytes  INTEGER      NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX hosted_tracks_owner_idx ON hosted_tracks (owner_id, created_at);

CREATE TABLE hosted_track_audio (
    track_id UUID PRIMARY KEY REFERENCES hosted_tracks (id) ON DELETE CASCADE,
    audio    BYTEA NOT NULL
);
