// Load test for the Negotiator API, replaying the calls the real UI makes per page view.
// The call lists mirror recorded-traffic.json, which `yarn e2e:record` (frontend/) regenerates
// from Playwright walks through the UI. See README.md in this directory for how to run it.
//
// Only run this against a local or dedicated test environment, never against production.

import http from "k6/http";
import { check, sleep, fail } from "k6";
import { Counter, Trend } from "k6/metrics";

const BASE = __ENV.BASE_URL || "http://localhost:8080";
const API = `${BASE}/api/v3`;
const PROFILE = __ENV.PROFILE || "load";

const RESOURCE = {
  id: "bbmri-eric:ID:SE_890:collection:dummy_collection",
  name: "Dummy test collection",
  organization: { externalId: "bbmri-eric:ID:SE_890", name: "Biobank Väst" },
};
const PAYLOAD = JSON.parse(open("./payload.json"));

// Access tokens from the sessions frontend/e2e/auth.setup.js stored (valid for one hour).
function token(role) {
  const { origins } = JSON.parse(open(`/auth/${role}.json`));
  const entry = origins
    .flatMap((origin) => origin.localStorage)
    .find(({ name }) => name.startsWith("oidc.user:"));
  return JSON.parse(entry.value).access_token;
}
const TOKENS = {
  researcher: token("researcher"),
  researcher2: token("researcher2"),
  biobanker: token("biobanker"),
  admin: token("admin"),
};

const sentTransitions = new Counter("lifecycle_events_sent");
const lostTransitions = new Counter("lifecycle_events_lost");
const pageView = new Trend("page_view_duration", true);

const STAGES = {
  smoke: [{ duration: "30s", target: 5 }],
  load: [
    { duration: "1m", target: 50 },
    { duration: "2m", target: 50 },
    { duration: "30s", target: 0 },
  ],
  stress: [
    { duration: "1m", target: 100 },
    { duration: "2m", target: 300 },
    { duration: "1m", target: 300 },
    { duration: "30s", target: 0 },
  ],
}[PROFILE];

// Share of virtual users per journey, roughly how the Negotiator is used: mostly reading.
const scenario = (exec, share) => ({
  executor: "ramping-vus",
  exec,
  stages: STAGES.map((s) => ({
    ...s,
    target: Math.max(1, Math.round(s.target * share)),
  })),
  gracefulRampDown: "10s",
});

export const options = {
  scenarios: {
    researcher: scenario("researcher", 0.45),
    representative: scenario("representative", 0.3),
    newRequest: scenario("newRequest", 0.15),
    admin: scenario("admin", 0.1),
  },
  thresholds: {
    http_req_failed: ["rate<0.01"],
    "http_req_duration{kind:read}": ["p(95)<500"],
    "http_req_duration{kind:write}": ["p(95)<1000"],
    page_view_duration: ["p(95)<1500"],
    lifecycle_events_lost: ["count==0"],
    "lifecycle_events_sent{event:SUBMIT}": ["count>=0"],
    "lifecycle_events_lost{event:SUBMIT}": ["count==0"],
    "lifecycle_events_sent{event:APPROVE}": ["count>=0"],
    "lifecycle_events_lost{event:APPROVE}": ["count==0"],
  },
};

function params(role, name, kind = "read") {
  return {
    headers: {
      Authorization: `Bearer ${TOKENS[role]}`,
      "Content-Type": "application/json",
    },
    tags: { name, kind },
  };
}

const get = (role, path, name) => [
  "GET",
  `${API}${path}`,
  null,
  params(role, name),
];

// The browser fires all requests of a page view in parallel; so does http.batch.
function view(name, requests) {
  const start = Date.now();
  const responses = http.batch(requests);
  pageView.add(Date.now() - start, { view: name });
  check(responses, {
    [`${name}: all 2xx`]: (rs) => rs.every((r) => r.status < 300),
  });
  responses.forEach(
    (r, i) => r.status >= 300 && reportFailure(requests[i][3].tags.name, r),
  );
  return responses;
}

function write(role, method, path, name, body) {
  const response = http.request(
    method,
    `${API}${path}`,
    body === undefined ? null : JSON.stringify(body),
    params(role, name, "write"),
  );
  check(response, { [`${name}: 2xx`]: (r) => r.status < 300 });
  if (response.status >= 300) reportFailure(name, response);
  return response;
}

function reportFailure(name, response) {
  console.warn(
    `FAILED ${name} -> ${response.status} ${String(response.body).slice(0, 200)}`,
  );
}

// Shell of every page: config, user, notifications (recorded on each navigation).
function shell(role, userId) {
  return [
    [
      "GET",
      `${BASE}/api/actuator/info`,
      null,
      params(role, "GET /actuator/info"),
    ],
    get(role, "/ui-config", "GET /ui-config"),
    get(role, "/ui-config", "GET /ui-config"),
    get(role, "/userinfo", "GET /userinfo"),
    get(role, "/userinfo", "GET /userinfo"),
    get(
      role,
      `/users/${userId}/notifications?page=0&size=10`,
      "GET /users/{id}/notifications",
    ),
  ];
}

function negotiationPage(role, userId, id) {
  return [
    ...shell(role, userId),
    get(role, `/negotiations/${id}`, "GET /negotiations/{id}"),
    get(
      role,
      `/negotiations/${id}/attachments`,
      "GET /negotiations/{id}/attachments",
    ),
    get(
      role,
      `/negotiations/${id}/resources`,
      "GET /negotiations/{id}/resources",
    ),
    get(
      role,
      `/negotiations/${id}/resources`,
      "GET /negotiations/{id}/resources",
    ),
    get(role, `/negotiations/${id}/posts`, "GET /negotiations/{id}/posts"),
    get(role, `/users/${userId}/resources`, "GET /users/{id}/resources"),
    get(
      role,
      `/negotiations/${id}/lifecycle`,
      "GET /negotiations/{id}/lifecycle",
    ),
    get(role, "/resource-lifecycle/states", "GET /resource-lifecycle/states"),
    get(
      role,
      `/negotiations/${id}/timeline`,
      "GET /negotiations/{id}/timeline",
    ),
  ];
}

const LIST_QUERY =
  "createdAfter=&createdBefore=&sortBy=creationDate&sortOrder=DESC&search=&page=0";

function listPage(role, userId, userRole) {
  const list =
    userRole === "admin"
      ? get(role, `/negotiations?${LIST_QUERY}`, "GET /negotiations")
      : get(
          role,
          `/users/${userId}/negotiations?role=${userRole}&${LIST_QUERY}`,
          "GET /users/{id}/negotiations",
        );
  return [
    ...shell(role, userId),
    get(
      role,
      "/negotiation-lifecycle/states",
      "GET /negotiation-lifecycle/states",
    ),
    list,
  ];
}

const think = () => sleep(1 + Math.random() * 3);
const pick = (items) => items[Math.floor(Math.random() * items.length)];

function createRequest(role) {
  const response = write(role, "POST", "/requests", "POST /requests", {
    url: "https://bbmritestnn.gcc.rug.nl",
    humanReadable: "k6 load test",
    resources: [RESOURCE],
  });
  return response.json("id");
}

function status(role, id) {
  return http
    .get(`${API}/negotiations/${id}`, params(role, "GET /negotiations/{id}"))
    .json("status");
}

// Lifecycle events can be dropped under concurrency while the API answers 200, so setup resends
// until the state changed; the journeys themselves never retry and count losses instead.
function transition(role, id, event, expected) {
  for (
    let attempt = 0;
    attempt < 10 && status(role, id) !== expected;
    attempt++
  ) {
    write(
      role,
      "PUT",
      `/negotiations/${id}/lifecycle/${event}`,
      `PUT lifecycle/${event}`,
      {},
    );
    sleep(0.3);
  }
  if (status(role, id) !== expected)
    fail(`setup: ${id} did not reach ${expected}`);
}

export function setup() {
  const userIds = {};
  for (const role of Object.keys(TOKENS)) {
    userIds[role] = http
      .get(`${API}/userinfo`, params(role, "setup"))
      .json("id");
  }
  // A pool of ongoing negotiations between Researcher and Biobanker to read and post to.
  const negotiations = [];
  for (let i = 0; i < 20; i++) {
    const request = createRequest("researcher");
    const id = write(
      "researcher",
      "POST",
      "/negotiations",
      "POST /negotiations",
      {
        request,
        payload: PAYLOAD,
        draft: true,
      },
    ).json("id");
    transition("researcher", id, "SUBMIT", "SUBMITTED");
    transition("admin", id, "APPROVE", "IN_PROGRESS");
    negotiations.push(id);
  }
  return { userIds, negotiations };
}

export function researcher({ userIds, negotiations }) {
  const me = userIds.researcher;
  view("researcher list", [
    ...listPage("researcher", me, "author"),
    get("researcher", `/users/${me}/networks`, "GET /users/{id}/networks"),
    get("researcher", `/users/${me}/networks`, "GET /users/{id}/networks"),
  ]);
  think();
  const id = pick(negotiations);
  view("negotiation page", negotiationPage("researcher", me, id));
  think();
  if (Math.random() < 0.3) {
    write(
      "researcher",
      "POST",
      `/negotiations/${id}/posts`,
      "POST /negotiations/{id}/posts",
      {
        text: `k6 researcher message ${__VU}-${__ITER}`,
        type: "PUBLIC",
      },
    );
    view("posts reload", [
      get(
        "researcher",
        `/negotiations/${id}/posts`,
        "GET /negotiations/{id}/posts",
      ),
    ]);
  }
  think();
}

export function representative({ userIds, negotiations }) {
  const me = userIds.biobanker;
  view("representative list", listPage("biobanker", me, "representative"));
  think();
  const id = pick(negotiations);
  view("negotiation page", negotiationPage("biobanker", me, id));
  think();
  if (Math.random() < 0.3) {
    write(
      "biobanker",
      "POST",
      `/negotiations/${id}/posts`,
      "POST /negotiations/{id}/posts",
      {
        text: `k6 representative answer ${__VU}-${__ITER}`,
        type: "PUBLIC",
      },
    );
    view("posts reload", [
      get(
        "biobanker",
        `/negotiations/${id}/posts`,
        "GET /negotiations/{id}/posts",
      ),
    ]);
  }
  think();
}

// A researcher handed over from the Directory fills in and submits a request; then an admin
// reviews and approves it.
export function newRequest({ userIds }) {
  const me = userIds.researcher2;
  const request = createRequest("researcher2");
  view("handover page", [
    ...shell("researcher2", me),
    get(
      "researcher2",
      `/users/${me}/negotiations?role=author&status=DRAFT&page=0`,
      "GET /users/{id}/negotiations",
    ),
    get("researcher2", `/requests/${request}`, "GET /requests/{id}"),
    get(
      "researcher2",
      `/requests/${request}/access-form`,
      "GET /requests/{id}/access-form",
    ),
  ]);
  const draft = write(
    "researcher2",
    "POST",
    "/negotiations",
    "POST /negotiations",
    {
      request,
      payload: PAYLOAD,
      draft: true,
    },
  );
  const id = draft.json("id");
  if (!id) return;
  view("edit form", [
    get("researcher2", `/negotiations/${id}`, "GET /negotiations/{id}"),
    get(
      "researcher2",
      `/negotiations/${id}/resources`,
      "GET /negotiations/{id}/resources",
    ),
    get(
      "researcher2",
      `/negotiations/${id}/access-form`,
      "GET /negotiations/{id}/access-form",
    ),
    get(
      "researcher2",
      `/negotiations/${id}/attachments`,
      "GET /negotiations/{id}/attachments",
    ),
    get("researcher2", "/discovery-services", "GET /discovery-services"),
  ]);
  // Each "Next" in the form wizard saves the draft.
  for (let step = 0; step < 5; step++) {
    think();
    write(
      "researcher2",
      "PATCH",
      `/negotiations/${id}`,
      "PATCH /negotiations/{id}",
      {
        payload: PAYLOAD,
      },
    );
  }
  write(
    "researcher2",
    "PUT",
    `/negotiations/${id}/lifecycle/SUBMIT`,
    "PUT lifecycle/SUBMIT",
    {},
  );
  sentTransitions.add(1, { event: "SUBMIT" });
  sleep(1);
  if (status("researcher2", id) !== "SUBMITTED")
    lostTransitions.add(1, { event: "SUBMIT" });
  view("negotiation page", negotiationPage("researcher2", me, id));
  think();

  view("admin list", listPage("admin", userIds.admin, "admin"));
  view("negotiation page", negotiationPage("admin", userIds.admin, id));
  think();
  if (status("admin", id) === "SUBMITTED") {
    write(
      "admin",
      "PUT",
      `/negotiations/${id}/lifecycle/APPROVE`,
      "PUT lifecycle/APPROVE",
      {},
    );
    sentTransitions.add(1, { event: "APPROVE" });
    sleep(1);
    if (status("admin", id) !== "IN_PROGRESS")
      lostTransitions.add(1, { event: "APPROVE" });
  }
}

export function admin({ userIds, negotiations }) {
  const me = userIds.admin;
  view("admin list", listPage("admin", me, "admin"));
  think();
  view("negotiation page", negotiationPage("admin", me, pick(negotiations)));
  think();
}
