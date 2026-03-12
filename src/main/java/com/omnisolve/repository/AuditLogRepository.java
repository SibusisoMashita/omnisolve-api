package com.omnisolve.repository;

import com.omnisolve.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for the {@code audit_logs} table.
 *
 * <p>Intentionally exposes only the {@link #save} method through the parent
 * interface. Audit logs are append-only; they must never be updated or deleted
 * via the application. Read access (for audit reporting) can be added here when
 * an admin API is built.
 */
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
