# Logging

The Negotiator uses **Logback** to handle logging.

## Default Configuration

Logback is configured, among other settings, in the **`application.yaml`** file.

By default, the **development** environment is configured to output logs only to the console, and the logging level is
set to **DEBUG**. For the **production** profile, the logging level is set to **INFO**.

For production environments, the default configuration can be found in
the [application file](https://github.com/BBMRI-ERIC/negotiator/blob/master/src/main/resources/application-prod.yaml).

The configuration uses a **SizeAndTimeBasedRollingPolicy**, which keeps the default time range (i.e., 1 day). This means
a new log file will be created each day. If the log file reaches the maximum file size (10MB) before the day ends, a new
log file is created, and the previous one is archived.

The file name pattern for logs is: **`negotiator.log.%d{%YYY-MM-dd}.%i.cz`**

To control storage consumption, the **max-history** parameter is configured to **30** (i.e., logs are retained for one
month), meaning that logs older than 30 days will be deleted automatically.

All configuration parameters can be overridden using environment variables.

By default, logs are stored in the **`/var/log/negotiator/`** directory.

## Accessing Logs

To view the logs from a running **Docker** container, you can use the following command:

```bash
docker logs <container_name_or_id>
```

Viewing Container Stats

To view the resource usage and stats of a running Docker container, such as CPU and memory usage, you can use the docker
stats command:

```shell
docker stats <container_name_or_id>
```

## Customizing Log Level through Environment Variables

In Spring applications, you can customize the log level dynamically through environment variables. This is particularly
useful when deploying to different environments (e.g., development, staging, production). To change the log level, you
can set the property in the environment.

```yaml
logging.level.<logger_name>
```

For example, to set the log level for the root
logger to **DEBUG**, you can use the environment variable:

```yaml
LOGGING_LEVEL_ROOT=DEBUG
```

Similarly, you can customize the
log level for specific packages or classes by specifying the package or class name. For instance, to set the log level
for the **com.example** package to **INFO**, use

```yaml
LOGGING_LEVEL_COM_EXAMPLE=INFO
```

These environment variables can be
passed in the **Docker Compose** file or directly to the application at runtime, allowing you to fine-tune logging
without modifying application configuration files.
