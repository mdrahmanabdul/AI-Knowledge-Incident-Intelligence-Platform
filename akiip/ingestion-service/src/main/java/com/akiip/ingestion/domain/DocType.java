package com.akiip.ingestion.domain;

/** What kind of knowledge a document holds. Drives chunking strategy and filters. */
public enum DocType {
    RUNBOOK,
    INCIDENT_REPORT,
    POSTMORTEM,
    API_SPEC,
    DESIGN_DOC,
    GUIDE,
    CODE,
    CHAT_THREAD,
    OTHER
}
