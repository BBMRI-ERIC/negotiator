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

## Development

For development, we advise to use a service called [Mailhog](https://github.com/mailhog/MailHog).
It serves to test sending email messages and acts as a mock smtp server.
For setup, follow the instructions mentioned in their documentation.