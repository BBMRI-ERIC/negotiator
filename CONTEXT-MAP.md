# Context Map

The BBMRI-ERIC Negotiator is split into two contexts, each with its own domain language and its own architectural decisions. This file points at where each one is documented — read the relevant context's `CONTEXT.md` before exploring its code, and use its vocabulary in anything you write.

## Contexts

| Context | Lives in | Glossary | Decisions |
| --- | --- | --- | --- |
| **Backend** — the Spring Boot service: negotiations over access to biobank resources, their lifecycles, and the governance structures (networks, representatives) around them. | `backend/` | [`backend/CONTEXT.md`](backend/CONTEXT.md) | `backend/docs/adr/` |
| **Frontend** — the Vue 3 single-page app users negotiate through. | `frontend/` | _not yet written_ | `frontend/docs/adr/` |

System-wide decisions — ones that span both contexts, or the shape of the repo itself — live in `docs/adr/`.

Directories listed above may not exist yet. They are created lazily, when the first term or decision actually needs recording; an absent one means nothing has been written there, not that something is missing.

## Which context owns what

The backend owns the domain: it is where lifecycle definitions, evaluation, resolution and pinning, information requirements, and governance live. `backend/CONTEXT.md` is the authority on those terms, and its `_Avoid_` lines are binding — don't drift to a synonym the glossary rejects.

The frontend consumes the backend's REST API and holds presentation concerns only. Where it needs a domain word, it takes the backend's.

## Not domain documentation

`docs/` at the repo root is the published user-facing documentation site (VitePress) — administrator and requester guides, deployment, auth. Useful as a description of current behaviour, but it is written for users, not as a domain model, and it is not the glossary.

`.scratch/` holds in-flight planning efforts (issues, PRDs, decision maps) per `docs/agents/issue-tracker.md`. Live thinking, not settled record: once a decision there is final it graduates into an ADR, and the glossary term into a `CONTEXT.md`.
