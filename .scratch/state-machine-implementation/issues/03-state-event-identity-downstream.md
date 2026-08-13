# State and Event identity for downstream consumers

Type: grilling
Status: open
Blocked by: 02

## Question

ADR 0002 deletes the four enums. Roughly **50 main-source files outside the state_machine package** — whatever ticket 02 does not relocate into Actions — still need to name a State or an Event. What do they use instead?

No ADR addresses this. It is the single largest unowned piece of stage 1.

### The consumers, by kind

**JPA queries and filters** — the hardest case, because the identifier reaches the database:
- `negotiation/NegotiationSpecification.java:80-95` — `hasState(List<NegotiationState>, boolean)` builds predicates against `root.get("currentState")`.
- Same file, `:33` and `:49` — hardcodes `NegotiationState.DRAFT` for a **visibility rule**, a semantic dependency on that specific state existing.
- `negotiation/dto/NegotiationFilterDTO.java:33` — `List<NegotiationState> status`, an API-level filter with enum validation today.
- `negotiation/NegotiationRepository.java`, `governance/resource/ResourceServiceImpl.java`.

**Aggregation** — `governance/network/stats/NetworkStatistics`, `SimpleNetworkStatistics`, `NetworkStatisticsServiceImpl` count negotiations by state.

**External contract** — `webhook/event/NegotiationStateUpdatedWebhookEvent`, `NegotiationResourceStateUpdatedWebhookEvent`, `NegotiationAddedWebhookEvent`. Relevant fact: enums already serialize as JSON strings, so the **wire format survives** a swap to `String`; only the OpenAPI enumerated values and `@Schema(example = "DRAFT")` metadata are affected.

**DTOs and mapping** — `negotiation/dto/NegotiationDTO`, `mappers/NegotiationModelMapper`, `mappers/NegotiationStatusConverter`, `mappers/NegotiationEventAssembler`, `governance/resource/dto/ResourceWithStatusDTO`, `ResourceViewDTO`, `negotiation/dto/UpdateResourcesDTO`, `negotiation/NegotiationTimelineImpl`.

**Entities** — `negotiation/Negotiation.java`, `negotiation/NegotiationResourceLink.java` hold the live state column. ADR 0009 fixes this one already: live state **stays an authoritative VARCHAR with no FK**, resolved through the natural key of the Definition Version Pin plus the state name. So the entity field is a string; the question is what the *type* is at the Java boundary.

Plus **26 of 138 test files**.

### Sharpen at least

1. **What is the type?** A bare `String`; a `StateName` / `EventName` value object wrapping a validated string; or keeping the enums as a *vocabulary* type decoupled from the definition rows, converted at the boundary. Note that a value object makes `switch` impossible — which is arguably *correct*, since you cannot exhaustively switch over configurable data.
2. **What happens to API-level validation?** `NegotiationFilterDTO` currently rejects an unknown state with a 400. Filtering on a string means an unknown value silently matches nothing. Is that acceptable, is it validated against the definition rows, or against a fixed vocabulary?
3. **The hardcoded `DRAFT` visibility rule.** The Negotiation Lifecycle keeps one definition so `DRAFT` is stable in practice — but the rule is a semantic dependency on a state name. Does it stay hardcoded, become a State flag, or something else? Note ADR 0002 already gives `State` `initial` and `terminal` flags, so "flags on State" is an established shape.
4. **Do statistics and filters need state *names* or state *ids*?** Ids are stable across label edits but meaningless across families; names are comparable across families but not unique globally.
5. **Is there a single answer, or does it differ per consumer kind?** An external webhook contract and an internal filter spec have genuinely different constraints.

Whatever is chosen is what the next slab decouples ~50 files onto, so the answer needs to be concrete enough to mechanically apply.

Use `/grilling` + `/domain-modeling` + `/codebase-design`. Any new term goes in `backend/CONTEXT.md`.
