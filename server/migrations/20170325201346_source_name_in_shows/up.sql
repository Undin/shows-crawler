ALTER TABLE shows
  ADD COLUMN source_name TEXT;

UPDATE shows
SET source_name = sources.name FROM sources
WHERE shows.source_id = sources.id;

ALTER TABLE shows
  ALTER COLUMN source_name SET NOT NULL;

ALTER TABLE shows
  DROP COLUMN source_id;

CREATE INDEX shows_source_name_index
  ON shows (source_name);
