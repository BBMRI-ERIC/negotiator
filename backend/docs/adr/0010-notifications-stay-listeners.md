---
status: accepted
---

# Notifications stay listeners

Keep notifications and webhooks as application-event listeners, not configurable Actions on Transitions. This preserves the existing behaviour without requiring notification Wiring on each Transition.

Listeners run asynchronously after the transaction commits, so delivery failures cannot roll back a Lifecycle move. State-specific listeners continue to use Well-known States; changing who hears what remains a code change.

Audience Notification follows the same approach. Spawn publishes an application event and leaves notification delivery to a listener.

Delivery remains in-memory: a crash after commit can lose a notification. A Definition Version without a Well-known State will not trigger the notification that depends on it.

## Considered options

- Keep application-event listeners (chosen): preserve the existing separation between Lifecycle changes and notification delivery.
- Make notifications configurable Actions. Rejected: delivery should not depend on Wiring each Transition.
