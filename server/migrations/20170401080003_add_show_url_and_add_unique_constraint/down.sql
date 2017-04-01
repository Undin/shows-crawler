ALTER TABLE shows
  DROP COLUMN show_url;

ALTER TABLE shows
  DROP CONSTRAINT shows_source_name_raw_id_unique;
