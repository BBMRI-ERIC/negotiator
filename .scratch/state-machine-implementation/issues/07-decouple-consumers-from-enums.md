# Decouple consumers from the lifecycle enums

Type: task
Status: open
Blocked by: 01, 02, 03

## Question

Migrate the consumer subsystems off the four lifecycle enums **while Spring Statemachine is still running**, one subsystem per commit. This is standing decision 2 — make the change easy, then make the easy change — and it is where the bulk of stage 1's file churn happens, deliberately done against a working system.

**Slab gate:** the full suite and ticket 01's characterization suite both green, and `NegotiationState`, `NegotiationResourceState`, `NegotiationEvent` and `NegotiationResourceEvent` referenced **only** inside `negotiation/state_machine/` and the three metadata DTOs (see carve-out below). No behaviour change of any kind — this slab moves types, not logic.

### Apply ticket 03's decision mechanically

Ticket 03 decides what names a State or Event. This slab applies it. Do not re-open that decision; if it turns out to be unworkable for a specific consumer, that is a finding to record against ticket 03, not a local improvisation.

### Suggested commit order — cheapest and most isolated first

1. **Webhook payloads** — `webhook/event/NegotiationStateUpdatedWebhookEvent`, `NegotiationResourceStateUpdatedWebhookEvent`, `NegotiationAddedWebhookEvent`, `NegotiationStateChangeWebhookMappingStrategy`, `NewNegotiationWebhookMappingStrategy`. Enums already serialize as JSON strings, so the wire format is unchanged — assert that in a test rather than assuming it. Update the `@Schema(example = …)` metadata.
2. **Network statistics** — `governance/network/stats/NetworkStatistics`, `SimpleNetworkStatistics`, `NetworkStatisticsServiceImpl`.
3. **DTOs and mappers** — `negotiation/dto/NegotiationDTO`, `mappers/NegotiationModelMapper`, `NegotiationStatusConverter`, `NegotiationEventAssembler`, `NegotiationModelAssembler`, `governance/resource/dto/ResourceWithStatusDTO`, `ResourceViewDTO`, `negotiation/dto/UpdateResourcesDTO`, `negotiation/NegotiationTimelineImpl`.
4. **Resource governance** — `governance/resource/ResourceServiceImpl`, `NonRepresentedResourcesHandlerImpl`.
5. **JPA filters and specs — the hardest, do it last.** `negotiation/NegotiationSpecification` (`hasState` at `:80-95`, and the hardcoded `NegotiationState.DRAFT` visibility rule at `:33` and `:49`), `negotiation/dto/NegotiationFilterDTO:33`, `negotiation/NegotiationRepository`. The `DRAFT` rule's disposition is settled by ticket 03 — apply it.
6. **The 26 test files** — churn alongside whichever commit touches their subject, not in one lump at the end.

### Two carve-outs

- **The three metadata DTOs are excluded** — `NegotiationStateMetadataDto`, `ResourceStateMetadataDto`, `ResourceEventMetadataDto`. They belong to ticket 04, which asks a different question (whether an endpoint enumerating a *universe* of States still makes sense). They are carved out precisely so this slab is not blocked on it, which is why the gate above names them.
- **Anything ticket 02 relocates into an Action is excluded.** Those consumers cannot move early — they need the Action registry, and therefore the schema and evaluator, to exist first. They belong to the cutover slab. Ticket 02's resolution defines this boundary exactly; read it before starting and record the resulting file list here.

### Note

Merge master into the branch before starting and again before finishing. This slab touches ~50 files across six subsystems and is the likeliest place to collide with concurrent work.
