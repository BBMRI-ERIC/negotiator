# Load tests

[k6](https://k6.io) scripts that replay the API traffic of real user journeys against the
Negotiator. Only run them against a local or dedicated test environment, never production.

## How the traffic is derived

`frontend/e2e/recording/journeys.spec.js` walks four journeys through the real UI with
Playwright and records every API call into `recorded-traffic.json`:

| Journey | Calls |
|---|---|
| Researcher opens their list, a negotiation, sends a message | 32 |
| Representative works through the biobank inbox | 28 |
| Admin reviews and approves a submitted negotiation | 33 |
| Researcher arrives from the Directory and submits a new request | 33 |

`negotiator.js` replays these calls per page view as parallel batches, like a browser, with
1–4 s think time between steps. Re-record after frontend changes and update the call lists:

```sh
cd frontend && yarn e2e:record
```

## Running

Start the stack (`docker compose up -d` in the repository root), then:

```sh
# Log the test users in; the access tokens are valid for one hour.
(cd frontend && npx playwright test --project=setup)

docker run --rm --user "$(id -u):$(id -g)" --network host \
  -v "$PWD/load:/scripts:ro" -v "$PWD/load/results:/results" \
  -v "$PWD/frontend/e2e/.auth:/auth:ro" \
  -e PROFILE=load grafana/k6:1.0.0 run --summary-export /results/summary.json /scripts/negotiator.js
```

| `PROFILE` | Virtual users | Duration |
|---|---|---|
| `smoke` | 5 | 30 s |
| `load` (default) | 50 | 3.5 min |
| `stress` | 100 → 300 | 4.5 min |

Users are split 45 % researchers, 30 % representatives, 15 % new requests (including the
admin approval), 10 % admins. `BASE_URL` overrides `http://localhost:8080`.

## Thresholds

Besides latency (p95 of reads < 500 ms, writes < 1 s, page views < 1.5 s) and errors (< 1 %),
every submit and approval is checked afterwards: `lifecycle_events_lost` counts events the API
acknowledged but did not apply, and must stay at 0.
