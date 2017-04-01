ALTER TABLE shows
  ADD COLUMN show_url TEXT NOT NULL DEFAULT '';

ALTER TABLE shows
  ADD CONSTRAINT shows_source_name_raw_id_unique UNIQUE (source_name, raw_id);
