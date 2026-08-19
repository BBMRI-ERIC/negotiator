# Transition authority cannot express "admin or creator"

Type: grilling
Status: open

## Question

ADR 0002 makes `Transition.required_authority` a **single** value of `NONE`, `IS_ADMIN`,
`IS_CREATOR`, `IS_REPRESENTATIVE`, `SYSTEM`. Six of the eight Negotiation Transitions cannot be
expressed by any of them.

Surfaced by slab [08](08-definition-schema-and-entities.md)'s expressiveness check against ticket
[01](01-freeze-current-behaviour.md)'s graph dump — see
[recon-expressiveness.md](../../definition-schema-and-entities/recon-expressiveness.md) part B gap 1.

### The fact

`SUBMIT`, `PAUSE`, `UNPAUSE`, `CONCLUDE` and **both** `ABANDON`s dump `securityRule: null`, but that
is a statement about the state machine bean, not about who may fire.
`NegotiationLifecycleServiceImpl.java:93-96` runs a blanket check *before* any rule is consulted:

```java
if (!roles.contains("ROLE_ADMIN")
    && !negotiationRepository.existsByIdAndCreatedBy_Id(negotiationId, userId)) {
  return Set.of();
}
```

So those six are firable by **admin or the Negotiation's creator, and by nobody else**. Against that:
`NONE` widens them to every authenticated caller — any user could abandon any Negotiation —
`IS_CREATOR` drops the admin, `IS_ADMIN` drops the creator. The two secured Transitions are fine:
`IS_ADMIN` is exactly right for `APPROVE`/`DECLINE`, because a non-admin creator passes the blanket
check and then fails the `ROLE_ADMIN` rule.

Before-picture [finding 7](../before-picture-findings.md) records the blanket check and that "an
admin and the creator are offered identical sets". It does **not** record the schema consequence, and
no Part 7 row covers it.

### The decision

Which of these, and why:

1. **The blanket check is not per-Transition authority at all** — it is uniform across the whole
   Negotiation Definition Scope, so it stays imperative at the service seam (or becomes a
   definition-level Guard row) and the six Transitions seed as `NONE`. Leaves ADR 0002 untouched.
   Note ADR 0002 deliberately separates "who is firing" from "is the move legal", which is an
   argument against the Guard variant specifically.
2. **Add a value** — `IS_ADMIN_OR_IS_CREATOR`. Cheapest DDL, but it is the first step onto a
   combinatorial ladder.
3. **Make the field set-valued** — the only option that changes the DDL slab 08 is writing, so if
   this is live, slab 08 needs to know before its migration lands.

### Two smaller variants of the same shape, recorded here rather than as their own ticket

- `ResourceLifecycleServiceImpl.isSecurityRuleMet:156-179` evaluates its three rules in a fixed
  `isCreator` → `isRepresentative` → `isAdmin` if/else chain. Harmless today — 0 Transitions carry
  more than one attribute — but it is the same "one value only" assumption baked into code.
- That same `isAdmin` branch returns `true` when `SecurityContextHolder`'s authentication is `null`.
  `IS_ADMIN` does not reproduce that.

### Note

`SYSTEM` is unexercised by both graphs (0 of 21). `CONCLUDE` is fired today *both* through
`runAsSystemUser` and by admin/creator from `IN_PROGRESS`, while ADR 0007 calls conclusion a System
Event. The column expresses either, so that is a seed decision rather than a contradiction — but it
is the same question wearing a different hat and is probably worth settling in the same conversation.
