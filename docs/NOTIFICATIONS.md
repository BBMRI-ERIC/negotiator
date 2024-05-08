# Notification service

The negotiator features a notification service for notifying representatives,
admins and researchers about various events tied to a Negotiation.

## Implementation

Currently, it's implemented
using [SpringMail](https://docs.spring.io/spring-boot/docs/2.0.0.M3/reference/html/boot-features-email.html).

## Configuration

To configure the Spring Mail, set the following environment variables or update the
properties file for the appropriate profile:

```
SPRING_MAIL_HOST="smtp.example"
SPRING_MAIL_PORT=1025
SPRING_MAIL_USERNAME="user"
SPRING_MAIL_PASSWORD="pass"
```

### Email template
Templates ensure consistent and automated communications for various functions. 
We are using [Thymeleaf](https://www.thymeleaf.org/) as the template engine, the email templates are stored in the `resources/templates/` directory.

### Email reminder
Automates the sending of emails based on schedules. The email reminder service is implemented using [Spring's Task Execution and Scheduling](https://spring.io/guides/gs/scheduling-tasks/). 
The reminder service is configured in the `application-prod.yml` file.
```yaml
reminder:
  trigger-duration-days: "P7D"
  cron-schedule-expression: "0 0 0 * * TUE"
```

## Development

For development, we advise to use a service called [Mailhog](https://github.com/mailhog/MailHog).
It serves to test sending email messages and acts as a mock smtp server.
For setup, follow the instructions mentioned in their documentation.