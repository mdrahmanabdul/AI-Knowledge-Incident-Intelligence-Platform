package com.akiip.ingestion.domain;

/** Data sensitivity. Later decides which LLM a chunk may be sent to (design doc §14.4). */
public enum Classification {
    PUBLIC,
    INTERNAL,
    CONFIDENTIAL,
    RESTRICTED
}
