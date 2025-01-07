# NegotiatorV3 Frontend

## Development Mode

To run the application in fully accessible development mode, you need to run an instance of
a [mock OIDC server](https://github.com/Soluto/oidc-server-mock). You can spin up a preconfigured instance with the
following command:

```sh
cd oidc_mock && docker-compose up -d
```

Then, to run the application with the correct settings, proceed as follows:

## Recommended Package Manager: Yarn

It is recommended to use Yarn as the package manager because some legacy packages (or packages that have not been
updated for a few years) specify a peer dependency that conflicts with packages you may want to install.

## Project Setup

To set up the project, you can use either Yarn or npm:

```sh
# Using Yarn
yarn install

# Using npm
npm install
```

### Compile and Hot-Reload for Development

To compile and hot-reload the application for development:

```sh
# Using Yarn
yarn dev

# Using npm
npm run dev
```

### Compile and Minify for Production

To compile and minify the application for production:

```sh
# Using Yarn
yarn build

# Using npm
npm run build
```

### Lint with [ESLint](https://eslint.org/)

To lint the codebase with ESLint:

```sh
# Using Yarn
yarn lint

# Using npm
npm run lint
```

### Run e2e tests with [Cypress](https://docs.cypress.io/guides/overview/why-cypress)

```sh
yarn cypress run --browser chrome
```

For more test scripts see [package.json](package.json)

### Customize Configuration

See the [Configuration Reference](https://cli.vuejs.org/config/) for more details on how to customize the configuration.

### Setting the Project Theme

Log in as Admin
Once logged in as an Admin, you can access the Admin UI Configuration via the drop-down menu or by navigating to the
page /ui-configuration. From there, you have full control over all UI-related settings.

Image Input Fields
For the image input fields, you have the option to chose a local file.
For example, you can use local image files, like:

* bbmri
* canserv
* eucaim
  Alternatively, you can link to external URLs for images if preferred.

  The logo files are located at:

   ```sh
   src/assets/images/
   ├── bbmri/
   │   ├── footer-bbmri.svg
   │   ├── home-bbmri.png
   │   └── nav-bar-bbmri.svg
   ├── canserv/
   │   ├── footer-canserv.png
   │   ├── home-canserv.png
   │   └── nav-bar-canserv.png
   └── eucaim/
       ├── footer-eucaim.png
       ├── home-eucaim.png
       └── nav-bar-eucaim.png
   ```

If you want to change an image, replace the existing one with a new one of the same name, file type, and size.

# Developer Guide - Configuration Files

## Configuration for Matomo Analytics

The Matomo configuration file is located at:

```sh
src/config/matomo.js
```

### The Matomo configuration involves two variables:

```sh
matomoHost: "MATOMO_HOST_PLACEHOLDER",
matomoId: "MATOMO_SITE_ID_PLACEHOLDER"
```

For more details, see
the [Matomo Configuration Reference](https://matomo.org/faq/new-to-piwik/how-do-i-install-the-matomo-tracking-code-on-websites-that-use-vue-js/).

## Configuration for Feature Flags

The feature flags configuration file is located at:

```sh
src/config/featureFlags.js
```

You can adjust the visibility of features, components, or code that is ready for testing but not intended for release by
using feature flags.

## Configuration for External Links

The external links configuration file is located at:

```sh
src/config/externalLinks.js
```

You can set the URLs that external links lead to in this file.

## Configuration for vuex-oidc

## Config for pinia-oidc

The OIDC configuration file is located at:

```sh
src/config/oidc.js
```

### About

This library provides OpenID Connect (OIDC) and OAuth2 protocol support for client-side, browser-based JavaScript client
applications. It also includes support for user session and access token management.

See [Configuration Reference](https://github.com/perarnborg/vuex-oidc/wiki, https://github.com/zhazhazhu/pinia-oidc).
