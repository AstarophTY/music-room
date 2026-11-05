-- Events: rooms where people propose songs and vote; the owner's Spotify plays the most voted song next.
-- visibility: 'public' or 'private' (owner and invited only).
-- vote_license: 'everyone', 'invited', or 'location' (within radius_m of latitude/longitude, between starts_at and ends_at).
-- status: 'waiting', 'playing' or 'paused'.
CREATE TABLE events (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id     UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name         VARCHAR(64)  NOT NULL,
    description  VARCHAR(280) NOT NULL DEFAULT '',
    visibility   VARCHAR(16)  NOT NULL DEFAULT 'public',
    vote_license VARCHAR(16)  NOT NULL DEFAULT 'everyone',
    latitude     DOUBLE PRECISION,
    longitude    DOUBLE PRECISION,
    radius_m     INTEGER,
    starts_at    TIMESTAMPTZ,
    ends_at      TIMESTAMPTZ,
    status       VARCHAR(16)  NOT NULL DEFAULT 'waiting',
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX events_owner_id_idx ON events (owner_id);
CREATE INDEX events_public_idx ON events (visibility, status);

-- People invited to an event (they see it when private, and vote with the 'invited' license).
CREATE TABLE event_invites (
    event_id   UUID        NOT NULL REFERENCES events (id) ON DELETE CASCADE,
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    invited_by UUID REFERENCES users (id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (event_id, user_id)
);

CREATE INDEX event_invites_user_id_idx ON event_invites (user_id);

-- Songs proposed in an event. track: the Spotify track details (JSON) read when proposed.
-- status: 'queued' (ordered by votes), 'next' (handed to Spotify to follow), 'playing', 'played'.
-- votes: kept equal to the number of event_votes rows, changed in the same transaction (votes = votes + 1).
CREATE TABLE event_tracks (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id    UUID        NOT NULL REFERENCES events (id) ON DELETE CASCADE,
    spotify_id  VARCHAR(64) NOT NULL,
    track       TEXT        NOT NULL,
    proposed_by UUID REFERENCES users (id) ON DELETE SET NULL,
    votes       INTEGER     NOT NULL DEFAULT 0 CHECK (votes >= 0),
    status      VARCHAR(16) NOT NULL DEFAULT 'queued',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    played_at   TIMESTAMPTZ
);

-- A song waits in an event only once (it can be proposed again after it played).
CREATE UNIQUE INDEX event_tracks_pending_idx ON event_tracks (event_id, spotify_id) WHERE status <> 'played';
CREATE INDEX event_tracks_queue_idx ON event_tracks (event_id, status, votes DESC, created_at);

-- One vote per user and song of an event: the unique key makes a double vote impossible, even at the same time.
CREATE TABLE event_votes (
    event_id       UUID        NOT NULL REFERENCES events (id) ON DELETE CASCADE,
    event_track_id UUID        NOT NULL REFERENCES event_tracks (id) ON DELETE CASCADE,
    user_id        UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (event_track_id, user_id)
);

CREATE INDEX event_votes_event_user_idx ON event_votes (event_id, user_id);

-- Notifications can be about an event (an invitation).
ALTER TABLE notifications ADD COLUMN event_id UUID REFERENCES events (id) ON DELETE CASCADE;
