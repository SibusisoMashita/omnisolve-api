package com.omnisolve.observability;

import com.omnisolve.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Servlet filter that instruments every HTTP request with structured logging
 * correlation IDs and request lifecycle metrics.
 *
 * <p>Runs at {@link org.springframework.core.Ordered#HIGHEST_PRECEDENCE} so
 * that correlation IDs are set before all other filters (including Spring
 * Security). This means every log statement written during request processing —
 * including authentication logs — carries the same {@code requestId}.
 *
 * <p><strong>MDC keys written by this filter:</strong>
 * <ul>
 *   <li>{@code requestId} — UUID unique per request; included in every log line</li>
 *   <li>{@code method}    — HTTP method (GET, POST, …)</li>
 *   <li>{@code path}      — request URI path</li>
 *   <li>{@code userId}    — populated later by
 *       {@link com.omnisolve.security.SecurityContextFacade} after auth resolves</li>
 *   <li>{@code tenantId}  — populated later by
 *       {@link com.omnisolve.security.SecurityContextFacade} after org resolves</li>
 * </ul>
 *
 * <p>{@code userId} and {@code tenantId} are cleared here (along with requestId)
 * in the {@code finally} block even though they are set elsewhere, because
 * this filter is the only guaranteed cleanup point for the thread.
 *
 * <p>{@link TenantContext#clear()} is also called here to remove the ThreadLocal
 * value before the thread is returned to the container pool.
 */
@Component
@Order(Integer.MIN_VALUE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    private static final String MDC_REQUEST_ID = "requestId";
    private static final String MDC_METHOD      = "method";
    private static final String MDC_PATH        = "path";
    private static final String MDC_USER_ID     = "userId";
    private static final String MDC_TENANT_ID   = "tenantId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String requestId = UUID.randomUUID().toString();
        long startNanos  = System.nanoTime();

        MDC.put(MDC_REQUEST_ID, requestId);
        MDC.put(MDC_METHOD, request.getMethod());
        MDC.put(MDC_PATH, request.getRequestURI());

        // Add requestId to response so clients can correlate on errors
        response.setHeader("X-Request-Id", requestId);

        log.debug("Request started: method={} path={}", request.getMethod(), request.getRequestURI());

        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
            log.debug("Request completed: method={} path={} status={} durationMs={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    durationMs);

            // Clear all MDC keys and ThreadLocals to prevent leakage into the pool
            TenantContext.clear();
            MDC.remove(MDC_REQUEST_ID);
            MDC.remove(MDC_METHOD);
            MDC.remove(MDC_PATH);
            MDC.remove(MDC_USER_ID);
            MDC.remove(MDC_TENANT_ID);
        }
    }
}
