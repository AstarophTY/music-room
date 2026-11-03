-- The same table now holds email verification codes and password reset codes.
ALTER TABLE email_verification_codes ADD COLUMN purpose VARCHAR(32) NOT NULL DEFAULT 'verify_email';

CREATE INDEX email_verification_codes_user_purpose_idx ON email_verification_codes (user_id, purpose);
