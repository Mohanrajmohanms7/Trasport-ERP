# Scaffolding & Environment Scaffolding Log (Phase 1)

This document details the environment configuration, workspace directory structure, application profile properties, and backend JWT security foundation established in Phase 1.

---

## 1. Verified Development Environment
- **Java Platform**: Java 21 LTS (Eclipse Temurin JDK 21.0.11+10).
- **Build Tool**: Apache Maven 3.9.6.
- **Node Environment**: Node.js & npm packages.
- **Frontend Framework**: Angular 20 Standalone.
- **Database Engine**: PostgreSQL 17 database service on port `5432`.
- **Version Control**: Git.

---

## 2. Directory Tree Structure
Created root directories under `d:\Mohan Programs\`:
- `/docs` - Markdown design guides and logging files.
- `/database` - DDL/DML SQL scripts and diagrams.
- `/scripts` - Backup and automation utilities.
- `/postman` - Collection test suites.
- `/design` - Figma screenshots and UX specifications.
- `/prompts` - Context prompts for AI tools.
- `/deployment` - Deployment config files.

---

## 3. Spring Boot Backend Profiles Configuration
We deleted the flat properties format and configured YAML-based multi-profile settings:
- **`application.yml`**: Exposes default active profile, date formatting, and common jackson timezones (UTC).
- **`application-dev.yml`**: Targets local PostgreSQL development parameters on port `5432`, mapping credentials to user `transport_admin`.
- **`application-test.yml`**: Exposes lightweight H2 in-memory settings for unit/integration testing suites.
- **`application-prod.yml`**: Maps production variables placeholders for passwords and URLs.

---

## 4. JWT Security Foundation
We added JWT support with `io.jsonwebtoken` JJWT 0.12.5 library:
- **JwtUtil.java**: Key parsing helper validating expiration limits and signing claims with SHA-256 keys.
- **CustomUserDetailsService.java**: Connects spring identity checks to the `AppUserRepository` and maps roles.
- **JwtAuthenticationFilter.java**: Web servlet filter intercepting incoming headers and setting security authentication contexts.
- **SecurityConfig.java**: Exposes Bcrypt hashing password encoder bean and stateless filter orderings.
- **OpenApiConfig.java**: Extends Swagger configurations to authorize API operations with Bearer tokens.
