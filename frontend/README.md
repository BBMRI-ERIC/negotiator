# Negotiator Frontend

This project uses Yarn as the package manager.
Yarn is recommended because some legacy packages specify peer
dependencies that can conflict with newer package versions when using npm.

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

To run end-to-end tests:

```sh
yarn cypress run --browser chrome
```

For additional test scripts, see [package.json](package.json).

### Customize Configuration

See the [Configuration Reference](https://cli.vuejs.org/config/) for more details on how to customize the configuration.

## Configuration for Matomo Analytics

The Matomo configuration file is located at:

```sh
src/config/matomo.js
```

### Matomo Configuration Variables

```javascript
matomoHost: "MATOMO_HOST_PLACEHOLDER",
    matomoId
:
"MATOMO_SITE_ID_PLACEHOLDER"
```

For more details, see
the [Matomo Configuration Reference](https://matomo.org/faq/new-to-piwik/how-do-i-install-the-matomo-tracking-code-on-websites-that-use-vue-js/).

## Configuration for Internationalization (i18n)

The i18n configuration involves two variables:

```javascript
locale: "I18N_LOCALE_PLACEHOLDER",
    fallbackLocale
:
"I18N_FALLBACKLOCALE_PLACEHOLDER"
```

Example configuration:

```javascript
locale: "en-bbmri",
    fallbackLocale
:
"en"
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

- [vuex-oidc](https://github.com/perarnborg/vuex-oidc/wiki)
- [pinia-oidc](https://github.com/zhazhazhu/pinia-oidc)
