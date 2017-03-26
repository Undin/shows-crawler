ALTER TABLE shows
  ADD COLUMN source_id BIGINT REFERENCES sources;

UPDATE shows
SET source_id = sources.id FROM sources
WHERE shows.source_name = sources.name;

ALTER TABLE shows
  ALTER COLUMN source_id SET NOT NULL;

ALTER TABLE shows
  DROP COLUMN source_name;
