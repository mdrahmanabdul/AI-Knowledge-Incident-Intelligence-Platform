package com.akiip.ingestion.domain;

/** Where documents come from. Only UPLOAD is used in the first month. */
public enum SourceType {
    UPLOAD,
    CONFLUENCE,
    GITHUB,
    JIRA,
    SLACK,
    PAGERDUTY,
    S3
}
