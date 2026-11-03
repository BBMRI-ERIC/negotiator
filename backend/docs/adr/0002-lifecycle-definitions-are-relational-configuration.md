---
status: accepted
---

# Lifecycle definitions are relational configuration

A Resource Lifecycle has to be configurable per Network or per Resource, so its definition has to be data rather than Java enums. The Negotiation Lifecycle uses the same model, so there is one model for both Definition Scopes.

A Definition Version stores each State, Event and Transition as its own row, so the database can reject a broken graph, such as a Transition that points at another version's State or a second Transition for the same State and Event.

The State and Event tables carry metadata such as label and description. A State also has a Progress Order to determine how advanced the State is within the graph.

Guards and Actions stay in Java, each with a type key and a params type. Only the Wiring is data. A Wiring row attaches a Guard or Action to a Transition, or a Guard to the whole Definition Version, with its params. Definition Compilation rejects params that do not fit, so a bad wiring row fails at compilation instead of at Event Firing.

Outside the Lifecycle, a State or Event name is a plain `String`, because a name only means something within a Definition Version. Java names a State only where behaviour depends on it, and these are the Well-known States.

## Considered options

- **A JSON or DSL document per Definition Version.** Rejected. The database could not reject a broken graph, so the application would have to validate the whole document on every write.
