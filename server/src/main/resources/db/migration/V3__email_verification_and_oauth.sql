-- When the user accepted the terms of use. NULL for accounts created before the terms existed.
ALTER TABLE users ADD COLUMN terms_accepted_at TIMESTAMPTZ;

-- 6-digit codes sent by email. Stored hashed, single use, short-lived, with a limited number of tries.
CREATE TABLE email_verification_codes (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    code_hash  VARCHAR(64) NOT NULL,
    attempts   INTEGER     NOT NULL DEFAULT 0,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX email_verification_codes_user_id_idx ON email_verification_codes (user_id);

-- Google / Apple identities linked to a user. provider_user_id is the token's `sub` claim.
CREATE TABLE oauth_accounts (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    provider         VARCHAR(16)  NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    email            VARCHAR(320),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (provider, provider_user_id),
    UNIQUE (user_id, provider)
);
