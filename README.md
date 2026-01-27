# ATLAS

### The Backbone of CollapseLoader

[![Java](https://img.shields.io/badge/Java-21-blue?logo=openjdk&logoColor=white)](https://jdk.java.net/21/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1.0--M1-brightgreen?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-latest-336791?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-Active-DC382D?logo=redis&logoColor=white)](https://redis.io/)
[![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?logo=docker&logoColor=white)](https://www.docker.com/)

**Atlas** is a high-performance, distributed backend system designed to power
the [CollapseLoader](https://github.com/dest4590/CollapseLoader) ecosystem. It handles everything from user
authentication and social interactions to achievement tracking and a global theme marketplace.

---

## 🚀 Features

Atlas provides a rich set of features through its RESTful API:

### 🔐 Multi-Layered Authentication

- **Secure Registration/Login**: Robust authentication with password hashing and JWT-based session management.
- **External Provider Sync**: Seamlessly link and manage multiple external game accounts.
- **Role-Based Access Control**: Granular permissions for Users and Admins.

### 🏆 Achievement System

- **Automated Progression**: Achievements are awarded in real-time based on high-level triggers (First Launch, Playtime,
  Social Activity).
- **Gamified Experience**: Transparent tracking of user milestones to boost engagement.

### 🎨 Marketplace & Presets

- **Theme Sharing**: A global repository where users can share, discover, and "like" custom UI presets.
- **Deep Integration**: Direct syncing with the CollapseLoader client for instant visual transformations.

### 🤝 Social & Friends

- **Dynamic Relations**: Comprehensive friendship management (requests, blocking, search).
- **Persistent Status**: Real-time presence tracking showing what clients friends are playing and for how long.
- **Social Links**: Deep-link social profiles (Discord, GitHub, Twitch) directly to your Atlas identity.

---

## 🛠️ Tech Stack

Atlas is built on a modern, reactive, and scalable stack:

- **Language**: [Java 21](https://jdk.java.net/21/)
- **Framework**: [Spring Boot 4.1.0-M1](https://spring.io/)
- **Persistence**: [JPA / Hibernate](https://hibernate.org/) with [PostgreSQL](https://www.postgresql.org/)
- **Caching**: [Redis](https://redis.io/) for high-speed session and status tracking
- **Security**: [Spring Security](https://spring.io/projects/spring-security) + [JWT](https://jwt.io/)
- **Deployment**: [Docker](https://www.docker.com/) & [Docker Compose](https://docs.docker.com/compose/)
- **Documentation**: [API_ENDPOINTS.md](API_ENDPOINTS.md) for the full REST endpoint list and request/response details.

---

## ⚡ Quick Start

### Prerequisites

- [Docker](https://www.docker.com/get-started)
- [Maven](https://maven.apache.org/download.cgi) (for local builds)

### Run with Docker Compose

```powershell
docker compose up -d
```

This will spin up the **Atlas API Server**, **PostgreSQL** database, and **Redis** instance automatically.

---

## 📂 Project Structure

```text
Atlas
├── src/main/java/org/collapseloader/atlas
│   ├── domain/           # Business logic grouped by feature
│   │   ├── achievements/ # Awards & Milestones
│   │   ├── auth/         # Security & Identity
│   │   ├── clients/      # Client downloads & stats
│   │   ├── presets/      # Customization Marketplace
│   │   └── users/        # Profile & Social Links
│   └── web/              # API Controllers & DTOs
├── compose.yaml          # Infrastructure configuration
└── API_ENDPOINTS.md      # Comprehensive API Reference
```

---

<div>
  <sub>Developed with ❤️ for the <b>CollapseLoader Community</b></sub>
</div>
