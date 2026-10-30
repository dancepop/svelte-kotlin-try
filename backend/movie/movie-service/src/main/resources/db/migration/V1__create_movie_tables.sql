-- Value rules (lengths, allowed values, size limits) are enforced in the application, not with CHECKs.
-- The database keeps only structure: types, NOT NULL, keys and foreign keys.

CREATE TABLE movies (
    id              uuid        NOT NULL DEFAULT uuidv7(),
    title           text        NOT NULL,
    synopsis        text,
    release_date    date,
    runtime_minutes integer,
    status          text        NOT NULL DEFAULT 'DRAFT',  -- DRAFT | PUBLISHED (validated by the application)
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now(),
    version         integer     NOT NULL DEFAULT 0,
    CONSTRAINT pk_movies PRIMARY KEY (id)
);
CREATE INDEX idx_movies_title_trgm ON movies USING gin (title gin_trgm_ops);

CREATE TABLE people (
    id         uuid        NOT NULL DEFAULT uuidv7(),
    name       text        NOT NULL,
    bio        text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    version    integer     NOT NULL DEFAULT 0,
    CONSTRAINT pk_people PRIMARY KEY (id)
);
CREATE INDEX idx_people_name_trgm ON people USING gin (name gin_trgm_ops);

CREATE TABLE credits (
    movie_id    uuid        NOT NULL,
    person_id   uuid        NOT NULL,
    credit_type text        NOT NULL,  -- CAST | CREATOR (validated by the application)
    created_at  timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_credits PRIMARY KEY (movie_id, person_id, credit_type),
    CONSTRAINT fk_credits_movie_id  FOREIGN KEY (movie_id)  REFERENCES movies (id) ON DELETE CASCADE,
    CONSTRAINT fk_credits_person_id FOREIGN KEY (person_id) REFERENCES people (id) ON DELETE CASCADE
);
-- Postgres doesn't index foreign keys automatically. movie_id is covered by the primary key
-- (its first column); person_id needs its own index for "movies of person X" and the cascade.
CREATE INDEX idx_credits_person_id ON credits (person_id);

CREATE TABLE movie_media (
    movie_id         uuid        NOT NULL,
    kind             text        NOT NULL,  -- ARTWORK | PREVIEW | FULL (validated by the application)
    storage_key      text        NOT NULL,
    content_type     text        NOT NULL,
    size_bytes       bigint      NOT NULL,
    duration_seconds integer,
    created_at       timestamptz NOT NULL DEFAULT now(),
    updated_at       timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_movie_media PRIMARY KEY (movie_id, kind),
    CONSTRAINT fk_movie_media_movie_id FOREIGN KEY (movie_id) REFERENCES movies (id) ON DELETE CASCADE,
    CONSTRAINT uq_movie_media_storage_key UNIQUE (storage_key)
);
-- No extra index: the primary key (movie_id, kind) already serves "media of movie X".
