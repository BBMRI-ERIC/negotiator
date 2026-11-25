# Governance

Governance is the page where it is possible to manage the **Organizations** and **Resources** in the Negotiator.

In this section an authorized user can:

  - create and update organizations;
  - create and update resources of organizations;
  - assign representatives of resources.

Organizations and resources are stored in the Negotiator database, and are created and updated there, in both modes:
either from this page or by a registered [Discovery Service](/connecting-to-negotiator). What the mode decides is
**where the assignment of representatives is recorded**:

| Mode | Where representative assignments are stored | Who manages them from the Governance page |
|------|---------------------------------------------|--------------------------------------------|
| `NEGOTIATOR` (default) | The Negotiator database | Administrators |
| `PERUN` | A [Perun](https://perun.aai.cesnet.cz/) Virtual Organization | Administrators and Network Managers, within the groups they are authorized to in Perun |

Both modes show the same Governance page at `/governance`, with its Organizations and Networks tabs. What changes is
where that page reads the representative assignments from and writes them to.

## Governance in the Negotiator

This is the default mode, and the one a plain Docker Compose deployment uses. Everything, including the representative
assignments, is in the Negotiator database, and the Governance page only talks to the Negotiator REST API:

* the organization and resource lists come from `/api/v3/organizations` and `/api/v3/organizations/{id}?expand=resources`;
* the user picker searches `/api/v3/users`;
* assigning or removing a representative writes to `/api/v3/users/{userId}/resources`.

Only users with the **ADMIN** role can manage governance in this mode.

## Governance in Perun

Set `GOVERNANCE_CLIENT=PERUN` when the people responsible for the resources are managed in Perun. Organizations and resources are still
created and maintained in the Negotiator; what moves to Perun is the membership of the representatives.

In Perun there are groups associated with the organizations and resources of the Negotiator. The association is made
through dedicated group attributes, whose values identify the corresponding organization or resource (see [./deployment.md#custom-properties-for-the-frontend-docker-container]).

### How the page behaves

1. An administrator or a Network Manager opens the Governance page.
2. The page asks Perun for all groups the logged-in user is allowed to see. Perun filters that result itself, so a
   manager only ever sees the part of the hierarchy they are authorized to manage.
3. The groups associated with an organization or a resource are kept, and the parent-child relations between them are
   used to build the hierarchy shown on the page.
4. For each of those groups, the matching organization is looked up in the Negotiator to get the data displayed: name, description, contact email and withdrawn state.
5. Opening an organization loads its resources from the Negotiator in the same way.
6. Choosing a resource for representative assignment opens a modal listing its current representatives, read from the
   members of that resource's representatives group in Perun.
7. The search for users to assign calls Perun, which matches one free-text query against both name and email. There is
   therefore a single search field in this mode, not two.
8. Confirming the selection registers the assignment in Perun, adding or removing those users in the representatives
   group of the resource.

A logged-in user who is only a **REPRESENTATIVE** can also open the page: the list of resources they represent is read
from the Negotiator API in both modes.

### Who can do what

| Action | `NEGOTIATOR` | `PERUN` |
|--------|--------------|---------|
| Create and update organizations and resources | Administrators | Administrators |
| Assign and remove representatives | Administrators | Administrators and Network Managers, for the groups they are authorized to in Perun |
| View the resources one represents | Representatives | Representatives |

In `PERUN` mode the Negotiator role and the Perun authorization both apply: a Network Manager can open the Governance
page, but the groups they see and can change are the ones Perun grants them.

### Calling the Perun API

The frontend calls the Perun API with the same JWT it uses for the Negotiator, under the `/perun` path prefix. That has
two consequences:

* the OIDC provider must be one that Perun also accepts, so the token is valid on both sides;
* the `/perun` prefix must be routed to the Perun API.

In development, the Vite development server does that routing, using the target configured in the frontend; see the
[frontend README](https://github.com/BBMRI-ERIC/negotiator/blob/master/frontend/README.md). In production it is the
job of the reverse proxy, because the frontend image deliberately bakes in no Perun URL and its own nginx
configuration has no `/perun` location.

Perun's API is a JSON-RPC endpoint reached with `POST` requests, so the proxy must forward the request body and the
`Authorization` header unchanged, and must strip the `/perun` prefix — the frontend sends
`/perun/groupsManager/addMember`, which has to arrive as `/groupsManager/addMember` on the Perun side. With nginx:

```nginx
location /perun/ {
  proxy_pass https://perun.example.org/;   # the trailing slash strips the /perun prefix
  proxy_set_header Authorization $http_authorization;
  proxy_ssl_server_name on;
}
```

With Traefik, the router for `/perun` needs a higher priority than the frontend router, which usually matches
`PathPrefix('/')`, plus a middleware that strips the prefix.

### Required configuration

When `GOVERNANCE_CLIENT` is `PERUN`, the Virtual Organization and the attribute names that identify the groups must be
configured as well: `VIRTUAL_ORGANIZATION`, `IDS_PREFIX`, `ORGANIZATION_ID_ATTR`, `RESOURCE_ID_ATTR`,
`ADMIN_ORGANIZATION_ID_ATTR`, `ADMIN_RESOURCE_ID_ATTR`, `GROUP_ATTR_BASE`, `USER_ATTR_BASE` and `EMAIL_ATTR`. Their
meaning and defaults are listed in the
[deployment guide](/deployment#custom-properties-for-the-frontend-docker-container).

The value stored in the group attribute is matched against the Negotiator record after the configured `IDS_PREFIX` is
stripped and dots are turned into colons, which is the convention BBMRI-ERIC uses for its identifiers: a Perun
collection name such as `bbmri-eric.ID.CZ_MMCI.collection.LTS` maps to the resource `sourceId`
`bbmri-eric:ID:CZ_MMCI:collection:LTS`.

## What the Negotiator knows about Perun

The backend has no governance switch and no Perun client. It always exposes the same governance API, and in `PERUN`
mode the frontend calls Perun instead of the representative-assignment endpoints, so those calls never reach the
Negotiator.

The Negotiator does not learn about an assignment made in Perun by itself: the **REPRESENTATIVE** and **NETWORK
MANAGER** roles, and the checks that decide who can act on a Negotiation, are all read from the Negotiator's own
tables. The two sides are kept aligned by a synchronization job that runs outside this repository: it reads the
representatives groups in Perun and records the matching assignments in the Negotiator database.

> [!NOTE]
> That job belongs to the deployment, not to the Negotiator. Without it, a `PERUN` instance still shows the correct
> representatives on the Governance page, but the users it appointed cannot act on the Requests they receive, because
> the Negotiator has no record of the assignment.

## Choosing a mode

* Use `NEGOTIATOR` when the instance is self-contained: administrators maintain the representatives inside the
  Negotiator, and no external system is involved.
* Use `PERUN` when the people responsible for the resources are already governed in Perun. The Negotiator delegates
  that part, and Network Managers can maintain the representatives of their own resources without being granted
  instance-wide administrative rights. This assumes a job that copies the assignments from Perun into the Negotiator,
  as described above.

::: warning
The mode is fixed when the frontend is built: the value of `GOVERNANCE_CLIENT` is baked into the bundle and
substituted at container start, so switching modes means redeploying the frontend container with the new value and its
Perun attributes. Representative assignments live in different stores in each mode and are not shared between them.
:::
