package eu.bbmri_eric.negotiator.email;

import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

@Configuration
@CommonsLog
class EmailRateLimitConfig {

  @Setter
  @Getter
  @Component
  @ConfigurationProperties(prefix = "negotiator.email.rate-limit")
  static class EmailRateLimitProperties {
    private int maxConcurrentConnections = 1;
    private int corePoolSize = 2;
    private int maxPoolSize = 4;
    private int queueCapacity = 2000;
    private int keepAliveSeconds = 120;
    private String threadNamePrefix = "email-sender-";
    private boolean waitForTasksToCompleteOnShutdown = true;
    private int awaitTerminationSeconds = 300;
    private int semaphoreTimeoutSeconds = 120;
    private long delayBetweenEmailsMs = 2000;
  }

  @Bean
  Semaphore emailRateLimitSemaphore(EmailRateLimitProperties properties) {
    int maxConnections = properties.getMaxConcurrentConnections();
    log.info(
        "Initializing email rate limit semaphore with %d concurrent connections allowed, %dms delay between emails"
            .formatted(maxConnections, properties.getDelayBetweenEmailsMs()));
    return new Semaphore(maxConnections, true);
  }

  @Bean(name = "emailTaskExecutor")
  Executor emailTaskExecutor(EmailRateLimitProperties properties) {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(properties.getCorePoolSize());
    executor.setMaxPoolSize(properties.getMaxPoolSize());
    executor.setQueueCapacity(properties.getQueueCapacity());
    executor.setKeepAliveSeconds(properties.getKeepAliveSeconds());
    executor.setThreadNamePrefix(properties.getThreadNamePrefix());
    executor.setWaitForTasksToCompleteOnShutdown(properties.isWaitForTasksToCompleteOnShutdown());
    executor.setAwaitTerminationSeconds(properties.getAwaitTerminationSeconds());
    executor.setRejectedExecutionHandler(
        (runnable, threadPoolExecutor) -> {
          log.warn(
              "Email task rejected. Queue size: %d, Active threads: %d, Pool size: %d"
                  .formatted(
                      threadPoolExecutor.getQueue().size(),
                      threadPoolExecutor.getActiveCount(),
                      threadPoolExecutor.getPoolSize()));
          runnable.run();
        });

    executor.initialize();

    log.info(
        "Email task executor initialized - Core pool: %d, Max pool: %d, Queue capacity: %d"
            .formatted(
                properties.getCorePoolSize(),
                properties.getMaxPoolSize(),
                properties.getQueueCapacity()));

    return executor;
  }
}
