# Negotiator Frontend

This project uses Yarn as the package manager.
Yarn is recommended because some legacy packages specify peer
dependencies that can conflict with newer package versions when using npm.

Yarn is pinned via `.yarnrc.yml` / `.yarn/releases` to a specific Berry
release — see `.yarnrc.yml`'s `yarnPath` for the exact version. If you
already have any Yarn on your PATH, this resolves automatically on
`yarn install` — no Corepack needed. If you don't have Yarn installed at
all, bootstrap it once with `corepack enable`, or run the pinned release
file directly (the `.cjs` under `.yarn/releases`, e.g. `node
.yarn/releases/yarn-<version>.cjs install`).

## Project Setup

To set up the project:

```sh
yarn install
```

### Compile and Hot-Reload for Development

To start the development server with hot-reload:

```sh
yarn dev
```

### Compile and Minify for Production

To build the application for production:

```sh
yarn build
```

### Lint with [ESLint](https://eslint.org/)

To lint the codebase:

```sh
yarn lint
```

### Run E2E Tests with [Cypress](https://docs.cypress.io/guides/overview/why-cypress)

The tests run against the docker compose stack. Start it from the repository root, then run the
suite from `frontend/`:

```sh
docker compose up -d
yarn cypress run --browser chrome
```

For additional test scripts, see [package.json](package.json).

#### Feature-flagged specs

`cypress.config.js` reads feature flags from the environment and passes them to the specs, so one
variable configures both the stack and the tests covering it. Specs for a disabled flag are
reported as pending instead of failing.

`FEATURE_FLAG_PDF_EXPORT_ENABLED` gates `cypress/e2e/Negotiation/downloadNegotiationPdf.cy.js`:

```sh
export FEATURE_FLAG_PDF_EXPORT_ENABLED=true
docker compose up -d
cd frontend && yarn cypress run --browser chrome
```

Use `export`. Prefixing only `docker compose up -d` sets the variable for compose but not for
Cypress, which shows the PDF buttons while the specs skip. CI sets the flag for the whole E2E job.

### Customize Configuration

See the [Configuration Reference](https://cli.vuejs.org/config/) for more details on how to customize the configuration.

## Configuration for Matomo Analytics

The Matomo configuration file is located at:

```sh
src/config/matomo.js
```

### Matomo Configuration Variables

```
matomoHost: "MATOMO_HOST_PLACEHOLDER",
matomoId: "MATOMO_SITE_ID_PLACEHOLDER"
```

For more details, see
the [Matomo Configuration Reference](https://matomo.org/faq/new-to-piwik/how-do-i-install-the-matomo-tracking-code-on-websites-that-use-vue-js/).

## Configuration for Internationalization (i18n)

The i18n configuration involves two variables:

```
locale: "I18N_LOCALE_PLACEHOLDER",
fallbackLocale: "I18N_FALLBACKLOCALE_PLACEHOLDER"
```

Example configuration:

```
locale: "en-bbmri", 
fallbackLocale: "en"
```

## Configuration for Feature Flags

The feature flags configuration file is located at:

```sh
src/config/featureFlags.js
```

You can control the visibility of features, components, or code that is ready for testing but not intended for release
by using feature flags.

## Configuration for External Links

The external links configuration file is located at:

```sh
src/config/externalLinks.js
```

Configure URLs for external links in this file.

## Configuration for OIDC Authentication

The OIDC configuration file is located at:

```sh
src/config/oidc.js
```

### About OIDC

This library provides OpenID Connect (OIDC) and OAuth2 protocol support for client-side, browser-based JavaScript
applications. It also includes support for user session and access token management.

See the configuration references:

- [oidc-client-ts](https://github.com/authts/oidc-client-ts)

## Configuration for Perun Governance

When representatives are managed in Perun (`GOVERNANCE_CLIENT=PERUN`), the frontend calls the Perun API under the
`/perun` prefix, as defined in `src/config/apiPaths.js`.

### Perun API URL for Development

In development the `/perun` proxy is provided by the Vite development server, and its target is read from
`VITE_PERUN_API_URL` in `vite.config.js`:

| Environment Variable | Description                                                                              | Default Value |
|----------------------|------------------------------------------------------------------------------------------|---------------|
| `VITE_PERUN_API_URL` | Base URL of the Perun API used as the target of the `/perun` development proxy.          | `none`        |

Start the development server in the Perun mode:

```sh
yarn dev:perun
```

It loads `.env.perun.development`, which is committed with placeholders, such as
`VITE_PERUN_API_URL='PERUN_API_PLACEHOLDER'`, that are not replaced automatically.

To use your own values without committing them, copy that file to `.env.perun.development.local` and edit the copy:

```sh
cp .env.perun.development .env.perun.development.local
yarn dev:perun:local
```

The `dev:perun:local` script loads only `.env.perun.development.local`, so the copy is what provides the values. Files
matching `.env.*.local` are ignored by git.

This variable is only used by the development server: production builds do not embed a Perun API URL, and the Perun
endpoint is wired up by the deployment instead. For the variables that configure a Perun-based deployment, see the
[Deployment guide](../docs/deployment.md#custom-properties-for-the-frontend-docker-container).
