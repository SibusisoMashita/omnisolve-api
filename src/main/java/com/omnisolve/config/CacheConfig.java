package com.omnisolve.config;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Cache configuration for OmniSolve.
 *
 * <p>Uses {@link ConcurrentMapCacheManager} as the backing store so that the
 * application starts and caches work correctly even without a Redis instance.
 * This is an intentional trade-off: cache entries are per-node and not
 * invalidated across Elastic Beanstalk instances, but this is acceptable for
 * the immutable reference data and low-velocity dashboard stats cached here.
 *
 * <p><strong>Upgrade path to Redis:</strong>
 * <ol>
 *   <li>Add {@code spring-boot-starter-data-redis} to {@code pom.xml}.</li>
 *   <li>Add {@code spring.data.redis.*} connection config to
 *       {@code application.yml}.</li>
 *   <li>Replace this bean with a {@code RedisCacheManager}; all
 *       {@code @Cacheable} / {@code @CacheEvict} call sites remain unchanged.</li>
 * </ol>
 *
 * <p><strong>Cache names and their intended contents:</strong>
 * <ul>
 *   <li>{@code clauses} — ISO clause reference data; changes very rarely</li>
 *   <li>{@code documentTypes} — document type reference data; rarely changes</li>
 *   <li>{@code departments} — department list; changes infrequently</li>
 *   <li>{@code incidentTypes} — incident type reference data</li>
 *   <li>{@code incidentSeverities} — severity levels</li>
 *   <li>{@code documentStats} — per-organisation dashboard stats; evicted on
 *       write operations</li>
 *   <li>{@code incidentDashboard} — per-organisation incident metrics; evicted
 *       on write operations</li>
 * </ul>
 */
@Configuration
@EnableCaching
public class CacheConfig {

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    /** Cache name for ISO clause reference data. */
    public static final String CLAUSES           = "clauses";
    /** Cache name for document type reference data. */
    public static final String DOCUMENT_TYPES    = "documentTypes";
    /** Cache name for department reference data. */
    public static final String DEPARTMENTS       = "departments";
    /** Cache name for incident type reference data. */
    public static final String INCIDENT_TYPES    = "incidentTypes";
    /** Cache name for incident severity reference data. */
    public static final String INCIDENT_SEVERITIES = "incidentSeverities";
    /** Cache name for per-organisation document dashboard statistics. */
    public static final String DOCUMENT_STATS    = "documentStats";
    /** Cache name for per-organisation incident dashboard metrics. */
    public static final String INCIDENT_DASHBOARD = "incidentDashboard";

    @Bean
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager manager = new ConcurrentMapCacheManager();
        manager.setCacheNames(List.of(
                CLAUSES,
                DOCUMENT_TYPES,
                DEPARTMENTS,
                INCIDENT_TYPES,
                INCIDENT_SEVERITIES,
                DOCUMENT_STATS,
                INCIDENT_DASHBOARD
        ));
        log.info("Cache manager initialized with caches: {}",
                List.of(CLAUSES, DOCUMENT_TYPES, DEPARTMENTS,
                        INCIDENT_TYPES, INCIDENT_SEVERITIES,
                        DOCUMENT_STATS, INCIDENT_DASHBOARD));
        return manager;
    }
}
