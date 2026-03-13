package com.omnisolve.config;

import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Asynchronous execution configuration for OmniSolve.
 *
 * <p>Enables Spring's {@code @Async} processing and configures a bounded
 * {@link ThreadPoolTaskExecutor} named {@code omnisolveAsync}. This executor is
 * used for:
 * <ul>
 *   <li>S3 file upload offloading (so HTTP responses are not blocked by I/O)</li>
 *   <li>Audit log persistence (fire-and-forget; business operations must not fail
 *       because audit writing is slow)</li>
 *   <li>Notification dispatch (email / webhook stubs, to be wired later)</li>
 * </ul>
 *
 * <p><strong>Future upgrade path:</strong> The {@code @Async} abstraction is a thin
 * layer over a plain {@link Executor}. Swapping to an SQS-backed or Kafka-backed
 * executor requires only changing this configuration class — all call sites remain
 * unchanged.
 *
 * <p>Thread pool sizing rationale:
 * <ul>
 *   <li>Core pool = 4 — baseline concurrency for a single-instance Beanstalk node</li>
 *   <li>Max pool = 16 — bounded burst capacity</li>
 *   <li>Queue capacity = 100 — back-pressure buffer before rejection</li>
 * </ul>
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    /**
     * Named executor bean for {@code @Async("omnisolveAsync")} annotations.
     *
     * <p>Using a named executor allows fine-grained control: critical async tasks
     * (audit) can be routed to a different pool than optional tasks (notifications)
     * in a future iteration.
     */
    @Bean(name = "omnisolveAsync")
    public Executor omnisolveAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("omnisolve-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        log.info("OmniSolve async executor initialized: corePool=4, maxPool=16, queueCapacity=100");
        return executor;
    }

    /**
     * Default executor used when {@code @Async} is used without a named qualifier.
     */
    @Override
    public Executor getAsyncExecutor() {
        return omnisolveAsyncExecutor();
    }

    /**
     * Global handler for uncaught exceptions thrown inside {@code @Async} methods.
     * Logs the failure so it is visible in CloudWatch without crashing the caller.
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) ->
                log.error("Uncaught exception in async method: method={}, error={}",
                        method.getName(), throwable.getMessage(), throwable);
    }
}
