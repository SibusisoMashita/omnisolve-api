# OmniSolve API — Project Memory

## Stack
- Spring Boot 3.3.5 / Java 21 / PostgreSQL / Flyway / AWS (S3, Cognito, Beanstalk)
- Build: Maven (`./mvnw`), tests run with embedded-postgres (no Docker needed)
- Unit tests: `*Test.java` (Surefire) | Integration tests: `*IT.java` (Failsafe)

## Key Architecture Decisions
- Multi-tenant SaaS: `organisation_id` on all business entities (documents, incidents, employees…)
- Auth: AWS Cognito JWT validated by `JwtSecurityConfig`; `JWT_ENABLED=false` disables for local dev
- Tenant resolution: `SecurityContextFacade.currentUser()` → DB lookup via employees table → returns `AuthenticatedUser` record
- Thread-local context: `TenantContext` holds resolved `organisationId` per request; cleared by `RequestLoggingFilter`
- Audit: `@Auditable` + `AuditAspect` → `AuditService.record(event)` (async, REQUIRES_NEW tx) → `audit_logs` table
- Events: `ApplicationEventPublisher` in services → `AuditEventListener` + `NotificationEventListener` (both `@Async`)
- Caching: `CacheConfig` with `ConcurrentMapCacheManager` (no Redis needed); constants in `CacheConfig.*`
- Async executor: named bean `omnisolveAsync` in `AsyncConfig`; all async work goes through it

## Package Map (new packages added in refactor)
- `audit/` — Auditable annotation, AuditAspect, AuditEvent, AuditService
- `event/` — domain event records + `listener/` (AuditEventListener, NotificationEventListener)
- `tenant/` — TenantContext (ThreadLocal)
- `observability/` — RequestLoggingFilter (sets requestId/tenantId/userId in MDC)
- `security/` — SecurityContextFacade (injectable), AuthenticatedUser (record), kept AuthenticationUtil for legacy

## Domain Entities
- Tenant-scoped: Document, Incident, Employee, Site, Role, AuditLog (all have organisation_id)
- Global reference: DocumentType, DocumentStatus, Department, Clause, IncidentType, IncidentSeverity, IncidentStatus, Permission

## Flyway Migrations
- V1: full schema DDL only (no data)
- V2: static global reference data (statuses, types, clauses, departments)
- V3+: domain data and new tables
- V5: incident management tables + seed reference data

## Test Infrastructure
- `TestSecurityConfig` sets `UsernamePasswordAuthenticationToken(TEST_USER_SUB)` per request
- `SecurityContextFacade` handles `UsernamePasswordAuthenticationToken` (principal is the sub string)
- `TestS3Config` provides a fake S3 client (no real AWS calls)

## Known Bugs Fixed
- `DocumentService.getWorkflowStats()` now uses org-scoped counts (was crossing tenant boundaries)

## pom.xml additions (this session)
- `spring-boot-starter-aop` (required for @Aspect / AuditAspect)
- `spring-boot-starter-cache` (required for @Cacheable / @CacheEvict / @EnableCaching)
