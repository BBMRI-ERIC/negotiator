# Notification Service

The negotiator features a comprehensive notification service for notifying users about various events tied to negotiations. The system is built using a strategy pattern with event-driven architecture to ensure extensibility and maintainability.

## Architecture Overview

The notification system consists of several key components:

### Core Components

1. **NotificationListener** - A generic event listener that receives Spring Application Events and dispatches them to appropriate handlers
2. **NotificationStrategy** - An interface that defines how specific events should be handled
3. **NotificationService** - The service responsible for creating and managing in-app notifications
### Notifications vs Emails

The system handles two types of communications:

- **Notifications** - In-app notifications stored in the database that users can view within the negotiator interface
- **Emails** - Email notifications sent to users' email addresses for important events

Both are triggered by the same events but serve different purposes.
Notifications provide an audit trail and in-app alerts, while emails ensure users are notified even when not actively using the platform.

### Event Flow

1. Business events (e.g., negotiation state changes, new posts) are published as Spring Application Events
2. The `NotificationListener` receives these events asynchronously via `@TransactionalEventListener`
3. The listener dispatches events to registered `NotificationStrategy` implementations
4. Each strategy handles its specific event type and creates appropriate notifications and/or emails

## Email Implementation

Email delivery is implemented using [SpringMail](https://docs.spring.io/spring-boot/docs/2.0.0.M3/reference/html/boot-features-email.html).

### Configuration

To configure Spring Mail, set the following environment variables or update the properties file for the appropriate profile:

```
SPRING_MAIL_HOST="smtp.example"
SPRING_MAIL_PORT=1025
SPRING_MAIL_USERNAME="user"
SPRING_MAIL_PASSWORD="pass"
```

### Email Templates

Email templates use [Thymeleaf](https://www.thymeleaf.org/) as the template engine for dynamic content.
Templates can be managed through the negotiator's admin interface.

## Developer Guide: Extending the Notification System

### Adding New Event Types and Handlers

To add support for new events:

#### 1. Create an Event Class
```java
public class MyCustomEvent extends ApplicationEvent {
    private final String negotiationId;
    
    public MyCustomEvent(Object source, String negotiationId) {
        super(source);
        this.negotiationId = negotiationId;
    }
    // Getters...
}
```

#### 2. Implement a Notification Strategy
```java
@Component
public class MyCustomEventHandler implements NotificationStrategy<MyCustomEvent> {
    private final NotificationService notificationService;

    @Override
    public Class<MyCustomEvent> getSupportedEventType() {
        return MyCustomEvent.class;
    }

    @Override
    @Transactional
    public void notify(MyCustomEvent event) {
        // Create notifications and/or trigger emails
        NotificationCreateDTO notification = new NotificationCreateDTO(
            userIds, "Title", "Body", event.getNegotiationId()
        );
        notificationService.createNotifications(notification);
    }
}
```

#### 3. Publish the Event
```java
@Service
public class MyBusinessService {
    private final ApplicationEventPublisher eventPublisher;

    public void performBusinessAction(String negotiationId) {
        // Business logic...
        eventPublisher.publishEvent(new MyCustomEvent(this, negotiationId));
    }
}
```

The `NotificationListener` will automatically discover and register new handlers, dispatching events to appropriate strategies.
