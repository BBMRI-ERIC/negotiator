---
name: focused-backend-tests
description: Use ONLY when running or debugging backend Java tests (Maven, -Dtest selectors, surefire reports). Prefer targeted runs through scripts/test-backend.sh and avoid full-suite runs unless explicitly requested.
---

# Focused Backend Tests

## Scope

Use this skill for backend test execution and triage in this repository.

- Trigger examples: "run this backend test", "mvn -Dtest", "surefire", "unit test", "integration test", "why did this test fail"
- Do not use for frontend or docs tests.

## Default strategy

1. Prefer focused selectors over broad test runs.
2. Use `scripts/test-backend.sh` instead of raw Maven commands.
3. Do not run the full backend suite unless the user explicitly asks for all tests.
4. Do not add `clean` unless explicitly requested.

## Commands

Run focused tests:

```bash
scripts/test-backend.sh NetworkTest
scripts/test-backend.sh 'RequestServiceTest#shouldCreate'
scripts/test-backend.sh '*Webhook*Test'
scripts/test-backend.sh 'NetworkTest,RequestServiceTest'
```

Run full suite only when explicitly requested:

```bash
scripts/test-backend.sh --all
```

Pass through extra Maven args after `--`:

```bash
scripts/test-backend.sh NetworkTest -- -Dspring.profiles.active=test
```

## Failure triage order

1. If present, inspect `backend/target/test-compile-errors.log` first.
2. Otherwise inspect `backend/target/surefire-reports/*.txt` and `backend/target/surefire-reports/TEST-*.xml`.
3. Summarize only key failure signals: failing class/method, exception type, and first actionable cause.

## Environment notes

- In the repo's `.#opencode` shell, run commands directly (do not wrap every command with `nix develop`).
- Integration and repository tests may require Docker due to Testcontainers (`@IntegrationTest`, `@RepositoryTest`).
