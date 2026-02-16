<div align="center">

<img align=center src="assets/atlas-logo.png" width="300px" alt="Atlas Logo">

<b>High-Performance Backend for <a href="https://github.com/dest4590/CollapseLoader">CollapseLoader</a></b>

</div>

Atlas is a distributed, reactive backend system engineered to power the [CollapseLoader](https://github.com/dest4590/CollapseLoader) ecosystem. It serves as the central authority for user identity, social interactions, with custom FS abstraction (Titan)

---

## System Architecture

Atlas is built on a microservices-ready architecture using Spring Boot 4.1.0 (M1) and Java 21, designed for high throughput and horizontal scalability.

### Titan File System

Titan is file system abstraction layer developed for Atlas. It provides a unified interface for managing static assets, including game clients, mods, and user-generated content.

**Key Technical Capabilities:**

- **Content Addressable Storage (CAS)**: Utilizes MD5 hashing to deduplicate files and ensure integrity.
- **Metadata Management**: Decouples file metadata (ownership, permissions, usage) from physical storage.
- **Resilient Operations**: implementing soft-delete with automated retention policies and trash lifecycle management.
- **Optimized Streaming**: Uses buffered `StreamingResponseBody` for efficient delivery of large binary artifacts (JARs, DLLs).

### Authentication & Security

- **Multi-Layered Security**: Implements Spring Security with custom JWT filters for stateless session management.
- **Role-Based Access Control (RBAC)**: Granular permission scopes for Administrators, Moderators, and standard Users.
- **OAuth2 Integration**: extensible support for linking external identity providers.

### Persistence & Caching

- **PostgreSQL**: Primary relational store for user data, social graphs, and marketplace transactions.
- **Redis**: High-performance caching layer for user sessions, presence tracking, and frequently accessed metadata.

---

## API Documentation

For complete details on all REST endpoints, request schemas, and response types, refer to the **[API_ENDPOINTS.md](API_ENDPOINTS.md)** file. This document serves as the authoritative reference for integrating with Atlas.

---

## Technology Stack

- **Core**: Java 21, Spring Boot 4.1.0-M1
- **Database**: PostgreSQL (Hibernate/JPA)
- **Cache**: Redis
- **Containerization**: Docker, Docker Compose
- **Build Tool**: Maven

---

## Deployment

Atlas is container-native and designed to be deployed via Docker Compose.

### Prerequisites

- Docker Engine
- Docker Compose Plugin

### Launch

```powershell
docker compose up -d
```

This command initializes the application container along with the required PostgreSQL and Redis services.

---

## Project Structure

```text
Atlas
├── src/main/java/org/collapseloader/atlas
│   ├── titan/            # Titan File System
│   ├── domain/           # All Atlas Logic, using DDD principles
│   └── config/           # Spring & Security Configuration
├── compose.yaml          # Container Orchestration
└── API_ENDPOINTS.md      # API Reference Documentation
```
