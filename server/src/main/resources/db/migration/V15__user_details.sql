-- What a user tells about themselves, by audience: everyone, their friends, or only themselves.
-- music_preferences: a JSON array of genres or moods.
CREATE TABLE user_details (
    user_id           UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    public_info       TEXT        NOT NULL DEFAULT '',
    friends_info      TEXT        NOT NULL DEFAULT '',
    private_info      TEXT        NOT NULL DEFAULT '',
    music_preferences TEXT        NOT NULL DEFAULT '[]',
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
