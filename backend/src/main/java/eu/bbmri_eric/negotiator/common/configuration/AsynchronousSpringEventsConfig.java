package eu.bbmri_eric.negotiator.common.configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

@Configuration
@CommonsLog
public class AsynchronousSpringEventsConfig {

  @Setter
  @Getter
  @Component
  @ConfigurationProperties(prefix = "negotiator.events.async")
  public static class AsyncEventsProperties {
    private int corePoolSize = 20;
    private int maxPoolSize = 10;
    private int queueCapacity = 100;
    private int keepAliveSeconds = 60;
    private String threadNamePrefix = "async-event-";
    private boolean waitForTasksToCompleteOnShutdown = true;
    private int awaitTerminationSeconds = 30;
  }

  @Bean(name = "asyncEventExecutor")
  @Primary
  @Profile("!test")
  public Executor asyncEventExecutor(AsyncEventsProperties properties) {
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
              "Async event execution rejected. Queue size: %d, Active threads: %d, Pool size: %d"
                  .formatted(
                      threadPoolExecutor.getQueue().size(),
                      threadPoolExecutor.getActiveCount(),
                      threadPoolExecutor.getPoolSize()));
          try {
            runnable.run();
          } catch (Exception e) {
            log.error("Failed to execute rejected async event synchronously", e);
            throw new RejectedExecutionException("Unable to execute async event", e);
          }
        });

    executor.initialize();

    log.info(
        "Async event executor initialized - Core pool: %d, Max pool: %d, Queue capacity: %d"
            .formatted(
                properties.getCorePoolSize(),
                properties.getMaxPoolSize(),
                properties.getQueueCapacity()));

    return executor;
  }

  @Bean(name = "applicationEventMulticaster")
  @Profile("!test")
  public ApplicationEventMulticaster applicationEventMulticaster(Executor asyncEventExecutor) {
    SimpleApplicationEventMulticaster eventMulticaster = new SimpleApplicationEventMulticaster();
    eventMulticaster.setTaskExecutor(asyncEventExecutor);
    eventMulticaster.setErrorHandler(
        throwable -> {
          log.error("Error occurred in async event processing", throwable);
        });
    return eventMulticaster;
  }
}
