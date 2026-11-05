-- Friendships. One row per pair: the user who asked (requester) and the one asked (addressee).
-- status: 'pending' until the addressee accepts, then 'accepted'.
CREATE TABLE friendships (
    requester_id UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    addressee_id UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    status       VARCHAR(16) NOT NULL DEFAULT 'pending',
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    accepted_at  TIMESTAMPTZ,
    PRIMARY KEY (requester_id, addressee_id),
    CHECK (requester_id <> addressee_id)
);

-- A pair has one row whoever asked first.
CREATE UNIQUE INDEX friendships_pair_idx ON friendships (LEAST(requester_id, addressee_id), GREATEST(requester_id, addressee_id));
CREATE INDEX friendships_addressee_id_idx ON friendships (addressee_id);

-- Chat messages between two friends. read_at: when the recipient saw it.
CREATE TABLE messages (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sender_id    UUID          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    recipient_id UUID          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    body         VARCHAR(2000) NOT NULL,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    read_at      TIMESTAMPTZ
);

CREATE INDEX messages_sender_recipient_idx ON messages (sender_id, recipient_id, created_at);
CREATE INDEX messages_unread_idx ON messages (recipient_id) WHERE read_at IS NULL;

-- What happened to a user: type 'friend_request', 'friend_accepted' or 'playlist_invite', by actor_id.
CREATE TABLE notifications (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type        VARCHAR(32) NOT NULL,
    actor_id    UUID REFERENCES users (id) ON DELETE CASCADE,
    playlist_id UUID REFERENCES playlists (id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    read_at     TIMESTAMPTZ
);

CREATE INDEX notifications_user_id_idx ON notifications (user_id, created_at DESC);
