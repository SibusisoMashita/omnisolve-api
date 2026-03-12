package com.omnisolve.audit;

import com.omnisolve.security.SecurityContextFacade;
import com.omnisolve.tenant.TenantContext;
import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * AOP aspect that intercepts methods annotated with {@link Auditable} and
 * delegates to {@link AuditService} after each successful invocation.
 *
 * <p>Entity ID extraction strategy (applied in order):
 * <ol>
 *   <li>If the return value is a Java record with an {@code id()} accessor, that
 *       value is used.</li>
 *   <li>If the first method parameter is a {@link UUID}, it is used as entity ID
 *       (matches the common {@code UUID id} parameter pattern in service methods).</li>
 *   <li>Falls back to {@code "unknown"}.</li>
 * </ol>
 *
 * <p>Audit persistence is delegated to {@link AuditService#record(AuditEvent)} which
 * runs asynchronously in a separate transaction, so audit failures never impact the
 * business call.
 */
@Aspect
@Component
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);

    private final AuditService auditService;
    private final SecurityContextFacade securityContextFacade;

    public AuditAspect(AuditService auditService, SecurityContextFacade securityContextFacade) {
        this.auditService = auditService;
        this.securityContextFacade = securityContextFacade;
    }

    /**
     * Around advice that executes the target method and, on success, builds and
     * submits an {@link AuditEvent}.
     */
    @Around("@annotation(auditable)")
    public Object audit(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        Object result = joinPoint.proceed();

        try {
            String userId        = securityContextFacade.currentUserId();
            Long   organisationId = TenantContext.getOrganisationId();
            String entityId      = extractEntityId(result, joinPoint.getArgs());

            AuditEvent event = new AuditEvent(
                    organisationId,
                    auditable.entityType(),
                    entityId,
                    auditable.action(),
                    userId,
                    OffsetDateTime.now(),
                    null);

            auditService.record(event);

        } catch (Exception ex) {
            // Never let audit instrumentation break the business call
            log.error("AuditAspect failed to build/submit audit event: method={}, error={}",
                    resolveMethodName(joinPoint), ex.getMessage(), ex);
        }

        return result;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String extractEntityId(Object returnValue, Object[] args) {
        // Strategy 1: return value has an id() accessor (Java record pattern)
        if (returnValue != null) {
            try {
                Method idMethod = returnValue.getClass().getMethod("id");
                Object id = idMethod.invoke(returnValue);
                if (id != null) {
                    return id.toString();
                }
            } catch (NoSuchMethodException ignored) {
                // Return type does not have id() — fall through to next strategy
            } catch (Exception ex) {
                log.debug("Could not invoke id() on return value: {}", ex.getMessage());
            }
        }

        // Strategy 2: first argument is a UUID (common pattern: method(UUID id, ...))
        if (args != null && args.length > 0 && args[0] instanceof UUID uuid) {
            return uuid.toString();
        }

        return "unknown";
    }

    private String resolveMethodName(ProceedingJoinPoint joinPoint) {
        MethodSignature sig = (MethodSignature) joinPoint.getSignature();
        return joinPoint.getTarget().getClass().getSimpleName() + "." + sig.getMethod().getName();
    }
}
