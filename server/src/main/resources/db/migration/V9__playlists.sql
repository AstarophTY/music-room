-- Playlists made in Music Room. type: 'personal' (only the owner edits) or 'shared' (members with write access edit).
CREATE TABLE playlists (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id         UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name             VARCHAR(64) NOT NULL,
    type             VARCHAR(16) NOT NULL DEFAULT 'personal',
    cover_updated_at TIMESTAMPTZ,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX playlists_owner_id_idx ON playlists (owner_id);

-- Users added to a playlist besides its owner. access: 'read' (listen) or 'write' (add and remove songs).
CREATE TABLE playlist_members (
    playlist_id UUID        NOT NULL REFERENCES playlists (id) ON DELETE CASCADE,
    user_id     UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    access      VARCHAR(16) NOT NULL DEFAULT 'read',
    added_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (playlist_id, user_id)
);

CREATE INDEX playlist_members_user_id_idx ON playlist_members (user_id);

-- Songs in order. track: the Spotify track details (JSON) read when it was added, so listing needs no Spotify call.
CREATE TABLE playlist_tracks (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    playlist_id UUID        NOT NULL REFERENCES playlists (id) ON DELETE CASCADE,
    spotify_id  VARCHAR(64) NOT NULL,
    track       TEXT        NOT NULL,
    position    INTEGER     NOT NULL,
    added_by    UUID REFERENCES users (id) ON DELETE SET NULL,
    added_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (playlist_id, spotify_id)
);

CREATE INDEX playlist_tracks_playlist_position_idx ON playlist_tracks (playlist_id, position);

-- Playlist photo, re-encoded as a square JPEG.
CREATE TABLE playlist_covers (
    playlist_id UUID PRIMARY KEY REFERENCES playlists (id) ON DELETE CASCADE,
    image       BYTEA       NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
