CREATE TABLE users (
  sub                  UUID PRIMARY KEY,
  username             VARCHAR(15) NOT NULL UNIQUE,
  display_name         VARCHAR(50),
  email                VARCHAR(255) NOT NULL UNIQUE,
  avatar_bytes         BYTEA,
  avatar_content_type  VARCHAR(20),
  created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX users_username_lower_idx ON users (lower(username));
