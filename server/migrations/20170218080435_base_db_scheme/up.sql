CREATE TABLE users (
  id INTEGER PRIMARY KEY,
  first_name TEXT NOT NULL,
  chat_id BIGINT NOT NULL,
  active BOOLEAN NOT NULL
);

CREATE TABLE sources (
  id BIGSERIAL PRIMARY KEY,
  name TEXT NOT NULL UNIQUE,
  url TEXT NOT NULL
);

CREATE TABLE shows (
  id BIGSERIAL PRIMARY KEY,
  source_id BIGINT NOT NULL REFERENCES sources,
  raw_id BIGINT NOT NULL,
  title TEXT NOT NULL,
  local_title TEXT,
  last_season INTEGER,
  last_episode INTEGER,
  UNIQUE (source_id, raw_id)
);

CREATE TABLE subscriptions (
  show_id BIGINT NOT NULL REFERENCES shows ON DELETE CASCADE,
  user_id INTEGER NOT NULL REFERENCES users ON DELETE CASCADE,
  PRIMARY KEY (show_id, user_id)
);

INSERT INTO sources (name, url) VALUES
  ('lostfilm', 'http://www.lostfilm.tv/'),
  ('newstudio', 'http://newstudio.tv/');
