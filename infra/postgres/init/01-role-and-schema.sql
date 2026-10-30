-- Runs once, as the superuser, when the Postgres volume is empty (docker-entrypoint-initdb.d).
-- Flyway creates the tables afterwards, as the movie role.

CREATE ROLE movie LOGIN PASSWORD 'movie';
CREATE SCHEMA movie AUTHORIZATION movie;

-- Trigram search. Extensions are per database; it goes in public.
CREATE EXTENSION IF NOT EXISTS pg_trgm;
