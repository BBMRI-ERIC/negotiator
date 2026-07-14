## Agent skills

### Issue tracker

Issues and PRDs live as local markdown files under `.scratch/<feature-slug>/`; no PR-as-request-surface (there's no GitHub/GitLab issue tracker in play here). See `docs/agents/issue-tracker.md`.

### Triage labels

Default role vocabulary (`needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`), recorded via a `Status:` line in each issue file. See `docs/agents/triage-labels.md`.

### Domain docs

Multi-context layout: `CONTEXT-MAP.md` at the root points to separate `backend/CONTEXT.md` and `frontend/CONTEXT.md`, each with its own `docs/adr/`. See `docs/agents/domain.md`.
