-- Who sees a playlist (visibility: 'public' or 'private') and who edits it (edit_license: 'everyone' or 'invited').
-- Playlists created before this migration were only reachable by their members: they stay private and invite-only.
ALTER TABLE playlists
    ADD COLUMN visibility   VARCHAR(16) NOT NULL DEFAULT 'private',
    ADD COLUMN edit_license VARCHAR(16) NOT NULL DEFAULT 'invited',
    -- Raised by each change, so a client can ignore a state older than the one it shows.
    ADD COLUMN revision     BIGINT      NOT NULL DEFAULT 0;

ALTER TABLE playlists
    ALTER COLUMN visibility SET DEFAULT 'public',
    ALTER COLUMN edit_license SET DEFAULT 'everyone',
    DROP COLUMN type;

CREATE INDEX playlists_public_idx ON playlists (visibility, updated_at DESC);
