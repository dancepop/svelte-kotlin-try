#!/bin/sh
# Adds the sample data to an empty database. The connection settings come from the PG* variables in docker-compose.yml.
set -e

# movie-service creates the tables through Flyway when it starts, which can be after this runs.
echo "Waiting for the tables. Start movie-service if it isn't running."
until [ "$(psql -tAc "SELECT to_regclass('movie.credits') IS NOT NULL")" = "t" ]; do
  sleep 2
done

# Only an empty database, so a later run can't bring back rows the user has deleted.
if [ "$(psql -tAc "SELECT EXISTS (SELECT 1 FROM movies) OR EXISTS (SELECT 1 FROM people)")" = "t" ]; then
  echo "The database already has data, so nothing was added."
  exit 0
fi

psql -v ON_ERROR_STOP=1 --single-transaction -f /seed/seed.sql
echo "Sample data added."
