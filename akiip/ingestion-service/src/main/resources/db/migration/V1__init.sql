-- =====================================================================
-- AKIIP schema v1 (Day 3)
-- Flyway runs this once and records it in flyway_schema_history.
-- NEVER edit this file after it has run: add V2__..., V3__... instead.
-- Design reference: design doc §11.3
-- =====================================================================

-- pgvector: adds the `vector` column type and similarity operators (<=>, <->)
CREATE EXTENSION IF NOT EXISTS vector;

-- ---------------------------------------------------------------------
-- 1. tenant: a company/workspace. Every tenant-scoped row carries tenant_id.
-- ---------------------------------------------------------------------
CREATE TABLE tenant (
    id          UUID        PRIMARY KEY,
    name        TEXT        NOT NULL,
    slug        TEXT        NOT NULL UNIQUE,
    status      TEXT        NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------------
-- 2. app_user: mirror of the Keycloak user (Keycloak keeps passwords).
-- ---------------------------------------------------------------------
CREATE TABLE app_user (
    id            UUID        PRIMARY KEY,
    tenant_id     UUID        NOT NULL REFERENCES tenant (id),
    idp_subject   TEXT        NOT NULL UNIQUE,      -- the "sub" claim of the JWT
    email         TEXT,
    display_name  TEXT,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_seen_at  TIMESTAMPTZ
);
CREATE INDEX idx_app_user_tenant ON app_user (tenant_id);

-- ---------------------------------------------------------------------
-- 3. source_connector: where documents come from (UPLOAD, CONFLUENCE, ...).
-- ---------------------------------------------------------------------
CREATE TABLE source_connector (
    id             UUID        PRIMARY KEY,
    tenant_id      UUID        NOT NULL REFERENCES tenant (id),
    type           TEXT        NOT NULL,
    name           TEXT        NOT NULL,
    config         JSONB       NOT NULL DEFAULT '{}'::jsonb,   -- non-secret settings
    secret_ref     TEXT,                                       -- pointer to a secret store, never the secret
    sync_schedule  TEXT,                                       -- cron expression
    sync_cursor    JSONB,                                      -- "where did the last sync stop"
    status         TEXT        NOT NULL DEFAULT 'ACTIVE',
    last_sync_at   TIMESTAMPTZ,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_source_connector_name UNIQUE (tenant_id, name)
);

-- ---------------------------------------------------------------------
-- 4. ingestion_job: one processing run (an upload, a sync, a re-index).
-- ---------------------------------------------------------------------
CREATE TABLE ingestion_job (
    id              UUID        PRIMARY KEY,
    tenant_id       UUID        NOT NULL REFERENCES tenant (id),
    source_id       UUID        NOT NULL REFERENCES source_connector (id),
    type            TEXT        NOT NULL,                -- UPLOAD / FULL / INCREMENTAL / REINDEX
    status          TEXT        NOT NULL,                -- QUEUED / RUNNING / SUCCEEDED / FAILED ...
    docs_seen       INT         NOT NULL DEFAULT 0,
    docs_changed    INT         NOT NULL DEFAULT 0,
    chunks_indexed  INT         NOT NULL DEFAULT 0,
    error_count     INT         NOT NULL DEFAULT 0,
    error_sample    JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    started_at      TIMESTAMPTZ,
    finished_at     TIMESTAMPTZ
);
CREATE INDEX idx_ingestion_job_source ON ingestion_job (source_id, created_at DESC);
CREATE INDEX idx_ingestion_job_active ON ingestion_job (status) WHERE status IN ('QUEUED', 'RUNNING');

-- ---------------------------------------------------------------------
-- 5. document: the logical document (survives many versions).
-- ---------------------------------------------------------------------
CREATE TABLE document (
    id                  UUID        PRIMARY KEY,
    tenant_id           UUID        NOT NULL REFERENCES tenant (id),
    source_id           UUID        NOT NULL REFERENCES source_connector (id),
    external_id         TEXT        NOT NULL,            -- id in the source system (page id, file path...)
    uri                 TEXT,
    title               TEXT        NOT NULL,
    doc_type            TEXT        NOT NULL,            -- RUNBOOK / POSTMORTEM / API_SPEC ...
    mime_type           TEXT,
    language            TEXT,
    classification      TEXT        NOT NULL DEFAULT 'INTERNAL',
    owner_team          TEXT,
    current_version_id  UUID,                            -- FK added below (circular reference)
    status              TEXT        NOT NULL DEFAULT 'ACTIVE',
    source_updated_at   TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at          TIMESTAMPTZ,
    CONSTRAINT uq_document_source_external UNIQUE (tenant_id, source_id, external_id)
);
CREATE INDEX idx_document_tenant_type ON document (tenant_id, doc_type, updated_at DESC);

-- ---------------------------------------------------------------------
-- 6. document_version: one row per content change. content_hash = SHA-256 hex.
-- ---------------------------------------------------------------------
CREATE TABLE document_version (
    id                 UUID        PRIMARY KEY,
    document_id        UUID        NOT NULL REFERENCES document (id) ON DELETE CASCADE,
    version_no         INT         NOT NULL,
    content_hash       VARCHAR(64) NOT NULL,
    raw_object_key     TEXT        NOT NULL,             -- where the original file sits in object storage
    parsed_object_key  TEXT,
    size_bytes         BIGINT,
    author             TEXT,
    status             TEXT        NOT NULL DEFAULT 'RECEIVED',
    embedding_model    TEXT,
    chunk_count        INT,
    ingested_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_document_version_no UNIQUE (document_id, version_no)
);
CREATE INDEX idx_document_version_hash ON document_version (document_id, content_hash);

-- document.current_version_id -> document_version.id
-- DEFERRABLE: the check runs at COMMIT, so a document and its first version
-- can be inserted in the same transaction in any order.
ALTER TABLE document
    ADD CONSTRAINT fk_document_current_version
    FOREIGN KEY (current_version_id) REFERENCES document_version (id)
    DEFERRABLE INITIALLY DEFERRED;

-- ---------------------------------------------------------------------
-- 7. chunk: retrievable pieces of a document version.
-- ---------------------------------------------------------------------
CREATE TABLE chunk (
    id                   UUID        PRIMARY KEY,
    tenant_id            UUID        NOT NULL REFERENCES tenant (id),
    document_id          UUID        NOT NULL REFERENCES document (id) ON DELETE CASCADE,
    document_version_id  UUID        NOT NULL REFERENCES document_version (id) ON DELETE CASCADE,
    parent_chunk_id      UUID        REFERENCES chunk (id) ON DELETE CASCADE,  -- small-to-big retrieval
    ordinal              INT         NOT NULL,                                 -- position inside the version
    chunk_level          TEXT        NOT NULL DEFAULT 'CHILD',                 -- PARENT / CHILD / SUMMARY
    section_path         TEXT,                                                 -- e.g. "Diagnosis > Step 2"
    content              TEXT        NOT NULL,
    context_prefix       TEXT,                                                 -- contextual retrieval (Day 27)
    token_count          INT         NOT NULL,
    page_no              INT,
    content_hash         VARCHAR(64) NOT NULL,
    metadata             JSONB       NOT NULL DEFAULT '{}'::jsonb,             -- error codes, http method...
    is_latest            BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_chunk_position UNIQUE (document_version_id, chunk_level, ordinal)
);
CREATE INDEX idx_chunk_document_latest ON chunk (document_id) WHERE is_latest;
CREATE INDEX idx_chunk_tenant_hash ON chunk (tenant_id, content_hash);

-- ---------------------------------------------------------------------
-- 8. chunk_embedding: the vector (meaning) of each chunk. 1024 dims = BGE-M3.
-- ---------------------------------------------------------------------
CREATE TABLE chunk_embedding (
    chunk_id    UUID          PRIMARY KEY REFERENCES chunk (id) ON DELETE CASCADE,
    embedding   VECTOR(1024)  NOT NULL,
    model       TEXT          NOT NULL,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now()
);
-- HNSW = fast approximate nearest-neighbour index, cosine distance (operator <=>)
CREATE INDEX idx_chunk_embedding_hnsw ON chunk_embedding USING hnsw (embedding vector_cosine_ops);

-- ---------------------------------------------------------------------
-- 9. outbox: events written in the SAME transaction as business data,
--    later published to Kafka by a relay (transactional outbox pattern).
-- ---------------------------------------------------------------------
CREATE TABLE outbox (
    id              BIGSERIAL    PRIMARY KEY,
    aggregate_type  TEXT         NOT NULL,     -- e.g. DOCUMENT
    aggregate_id    UUID         NOT NULL,
    event_type      TEXT         NOT NULL,     -- e.g. document.raw.v1
    payload         JSONB        NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    published_at    TIMESTAMPTZ
);
CREATE INDEX idx_outbox_unpublished ON outbox (id) WHERE published_at IS NULL;
