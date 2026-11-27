package com.akiip.ingestion.domain;

/** Pipeline progress of one document version: RECEIVED -> PARSED -> CHUNKED -> INDEXED. */
public enum VersionStatus {
    RECEIVED,
    PARSED,
    CHUNKED,
    INDEXED,
    FAILED
}
