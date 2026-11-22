# AKIIP

AI Knowledge and Incident Intelligence Platform: a Maven multi-module Spring Boot workspace.

## Requirements

- Java 21
- Maven 3.9+

## Build

```bash
mvn clean verify
```

The root POM manages Spring Boot, Spring AI, and Testcontainers versions. Java services and shared libraries are Maven modules. Contracts, evaluation, MCP servers, web app, deploy assets, and docs hold assets for their respective stacks.
