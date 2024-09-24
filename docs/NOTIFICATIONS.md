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
We are using [Thymeleaf](https://www.thymeleaf.org/) as the template engine, which allows us to create dynamic and customizable email content.

#### Template location
The email template is stored in the `src/main/resources/templates/` directory. It is written in HTML with Thymeleaf syntax, allowing you to include dynamic content using expressions, conditionals, and loops.

#### Editing the template
 - To edit the template, open the `email-notification.html` file.
 - Make the necessary changes to the template. You can use Thymeleaf syntax to include dynamic variables, iterate over data collections, or conditionally display content.
 - Save the changes to the file.
 - The template is now ready to be used in the email service.

##### Upload new template to a docker container
To upload a new template to a docker container, you can use the following command:
```bash
docker cp src/main/resources/templates/email-notification.html negotiator:/app/src/main/resources/templates/email-notification.html
```
Or when using docker-compose mount the volume to the container:
```yaml 
volumes:
  - ./src/main/resources/templates/email-notification.html:/app/src/main/resources/templates/email-notification.html
```
  

#### Template variables
The email template uses variables to include dynamic content. These variables are replaced with actual values when the email is sent. The following variables are available in the template:
 - `recipient`: The name of the recipient.
 - `frontendUrl`: The URL of the frontend application.
 - `negotiations`: List of negotiation Ids.
 - `roleForNegotiation`: List of roles for each negotiation.
 - `titleForNegotiation`: List of titles for each negotiation.
 - `notificationsForNegotiation`: List of notifications for each negotiation.
 - `emailYoursSincerelyText`: The text for the "Yours sincerely" section.
 - `emailHelpdeskHref`: The URL for the helpdesk.

#### Updating Templates at Runtime
Thymeleaf supports runtime updates to email templates. This allows you to make changes without restarting the application, which is useful for quick updates or tweaks:
 - **Enable Template Cache Refresh:** By default, Thymeleaf caches templates for performance. To enable runtime updates, you need to configure Thymeleaf to refresh templates automatically. This can typically be done by setting the `spring.thymeleaf.cache` property to `false` in your application configuration file (`application.yml`):
```yaml
spring.thymeleaf.cache=false
```
 - **Hot Reload:** If using a development environment that supports hot reloading (like Spring Boot DevTools), you can take advantage of this feature to see changes immediately after saving the template files.
 - **Production Considerations:** While runtime updates are useful in development, it's recommended to enable caching in production to improve performance. Ensure templates are thoroughly tested before deploying updates in a production environment.

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