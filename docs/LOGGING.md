# Logging

The Negotiator uses Logback to handle logging.

## Default Configuration

Logback is configured, among other configurations, in application.yaml file.

By default, the development environment is configured to output logs only to console and 
the logging level is set to DEBUG

For production, the default configuration is the following:

```yml
logging:
  file:
    name: "/var/log/negotiator/negotiator.log"
  pattern:
    console: "%d %-5level %logger : %msg%n"
    file: "%d %-5level [%thread] %logger : %msg%n"
  logback:
    rollingpolicy:
      max-file-size: 10MB
      total-size-cap: 1GB
      max-history: 30
  level:
    root: info
    org.springframeworf.web: info
    org.springframework.security: info
    eu.bbmri_eric.negotiator: info
```

The configuration uses a `SizeAndTimeBasedRollingPolicy` keeping the default configuration for time range (i.e., 1 day). 
It means that each day a new log file will be created, but if the log reaches the maximum file size (which is 10MB) before 
the day, it will create a new file for that day and archive the first file.
The file name pattern is `negotiator.log.%d{%YYY-mm-dd}.%i.cz`
To control the storage consumption `max-history` parameter is configured to 30 (i.e., one month) meaning that 
the history of 30 days will be kept and older logs are deleted.
All configuration parameters can be overriden as usual with environment variables.
Fially, by default, the logs are stored in `/var/log/negotiator/` directory. 

  

