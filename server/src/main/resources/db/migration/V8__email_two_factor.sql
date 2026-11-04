-- Two-factor authentication by email: off by default, the user turns it on from the settings.
ALTER TABLE users ADD COLUMN email_two_factor_enabled BOOLEAN NOT NULL DEFAULT FALSE;
