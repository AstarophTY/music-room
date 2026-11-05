-- Display names become unique (whatever the case): they identify users in search, friends and chat.
-- Older accounts that share a name keep the oldest one as is; the others get the start of their id appended.
UPDATE users u
SET display_name = left(u.display_name, 55) || '-' || left(u.id::text, 8)
FROM (
    SELECT id, row_number() OVER (PARTITION BY lower(display_name) ORDER BY created_at, id) AS rank
    FROM users
) ranked
WHERE u.id = ranked.id AND ranked.rank > 1;

CREATE UNIQUE INDEX users_display_name_lower_idx ON users (lower(display_name));
