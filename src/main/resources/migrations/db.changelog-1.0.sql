-- liquibase formatted sql

-- changeset luca:create-journal-1
CREATE TABLE IF NOT EXISTS public.journal
(
    ordering        BIGSERIAL,
    persistence_id  VARCHAR(255)               NOT NULL,
    sequence_number BIGINT                     NOT NULL,
    deleted         BOOLEAN      DEFAULT FALSE NOT NULL,
    tags            VARCHAR(255) DEFAULT NULL,
    message         BYTEA                      NOT NULL,
    PRIMARY KEY (persistence_id, sequence_number)
);

-- changeset luca:create-journal-2
CREATE UNIQUE INDEX journal_ordering_idx ON public.journal (ordering);

-- changeset luca:create-snapshot
CREATE TABLE IF NOT EXISTS public.snapshot
(
    persistence_id  VARCHAR(255) NOT NULL,
    sequence_number BIGINT       NOT NULL,
    created         BIGINT       NOT NULL,
    snapshot        BYTEA        NOT NULL,
    PRIMARY KEY (persistence_id, sequence_number)
);

-- changeset luca:create-access-list
create table if not exists public.access_grants
(
    id          varchar(255) not null,
    title       varchar(255) not null,
    icon        varchar(255),
    user_name   varchar(8),
    primary key (id, user_name)
);

-- changeset luca:create-offset-store
create table if not exists public.offset_store
(
    consumer_name varchar(255) not null,
    last_offset bigint not null,
    primary key (consumer_name)
)