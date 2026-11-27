package com.akiip.ingestion.domain;

/**
 * PARENT = big section given to the LLM, CHILD = small piece we search with,
 * SUMMARY = one-chunk summary (used for incidents). See design doc §8.2.
 */
public enum ChunkLevel {
    PARENT,
    CHILD,
    SUMMARY
}
