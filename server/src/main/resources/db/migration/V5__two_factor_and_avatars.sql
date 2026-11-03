-- Two-factor authentication (TOTP). Secrets are encrypted (AES-GCM) before they are stored.
ALTER TABLE users ADD COLUMN totp_secret VARCHAR(255);
ALTER TABLE users ADD COLUMN totp_pending_secret VARCHAR(255);
-- Last 30-second step accepted: a code cannot be replayed.
ALTER TABLE users ADD COLUMN totp_last_step BIGINT;
ALTER TABLE users ADD COLUMN two_factor_failures INTEGER NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN two_factor_locked_until TIMESTAMPTZ;

-- Single-use backup codes, stored hashed.
CREATE TABLE recovery_codes (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    code_hash  VARCHAR(64) NOT NULL,
    used_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX recovery_codes_user_id_idx ON recovery_codes (user_id);

-- Profile picture: a color for the initials, and an optional photo (re-encoded 256x256 JPEG).
ALTER TABLE users ADD COLUMN avatar_color VARCHAR(16) NOT NULL DEFAULT 'yellow';
ALTER TABLE users ADD COLUMN avatar_updated_at TIMESTAMPTZ;

CREATE TABLE user_avatars (
    user_id    UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    image      BYTEA       NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
