# Negotiator v3.21.0

**Release Date:** April 2026  
**Full Changelog:** [v3.20.0...v3.21.0](https://github.com/BBMRI-ERIC/negotiator/compare/v3.20.0...v3.21.0)

## What's New

### Merge Drafted Requests

![Merge Drafted Requests](/public/v3.21.0-merge_drafted_requests.gif)

You can now **merge drafted negotiation requests** into existing negotiations. This allows you to combine related access requests, reducing duplication and streamlining your workflow. Draft negotiations also feature:

- **Auto redirect to edit mode** when opening draft negotiations
- **Resource removal** from draft negotiations
- **Draft deletion** support
- Visibility controls for adding resources based on creation date

### Discovery Services for Additional Resources

![Available Discovery Services](/public/v3.21.0-available_discovery_services.gif)

A new feature lists **available discovery services** when adding additional resources to a negotiation, making it easier to find and include the right resources from connected services.

### Privacy and Access Policy Configuration

Administrators can now configure **Privacy Policy** and **Access Policy** links directly from the backend. These settings are stored in the database with a dedicated migration, providing a centralized way to manage legal and compliance links displayed to users.

## User Interface Improvements

- Small UI style improvements across the platform
- Fixed notification badge number sizing for better readability
- Improved **avatar display**: acronym logic now correctly uses the first initial of the first and last word, and displays only two letters
- Updated navigation and resource display for clarity and consistency
- Vue Tour component rewritten from Options API to Composition API for better maintainability

### Webhooks System

A comprehensive **webhooks system** has been introduced, enabling external systems to receive real-time notifications about events in the Negotiator. Key capabilities include:

- **Event-driven notifications**: Automatically trigger webhooks on negotiation state changes and resource events
- **Structured payloads**: Well-defined data models with consistent event type, timestamp, and state transition fields
- **Re-delivery support**: Failed webhook deliveries can be retried with a dedicated redeliver button
- **Improved error handling**: Better handling of delivery failures with detailed status tracking
- **Simplified management UI**: Cleaner webhook display for easier configuration

## Bug Fixes

- **SSL Verification**: Safer SSL verification bypass for test configurations
- **Access Forms**: Fixed required and applicable fields; fixed initial load for option-only access forms
- **Authentication**: Fixed expired access token handling and implemented silent token renewal callbacks
- **Network Manager**: Fixed permissions for network managers to access negotiations in their network
- **Login Page**: Fixed spacing and overflow issues
- **Privacy Policy**: Fixed link migration and defaults; handle empty link configurations gracefully
- **Notifications**: Fixed notification text and replaced incorrect alert type
- **Headers**: Safe and robust header naming for legacy browsers
- **Frontend Build**: Process placeholders for all JS chunk files; convert absolute URLs to relative paths

## For Administrators

### Deployment Updates

- Updated Trivy action version for security scanning
- Debug mode available in development environment
- Test certificate error messages for easier SSL troubleshooting

## Dependency Updates

We've updated various components to their latest versions for improved security and performance:

- Vue 3.5.22 → 3.5.27
- Vue Router 4.5.0 → 5.0.1
- oidc-client-ts 3.1.0 → 3.4.1
- @vitejs/plugin-vue 5.2.1 → 6.0.3
- @eslint/js 9.39.0 → 10.0.1
- eslint-plugin-vue 9.32.0 → 9.33.0
- axios 1.13.5 → 1.13.6
- core-js 3.47.0 → 3.48.0
- Multiple security patches across npm and GitHub Actions dependencies

*Thank you to all contributors who made this release possible: [@tmilost](https://github.com/tmilost), [@stetsche](https://github.com/stetsche), [@RadovanTomik](https://github.com/RadovanTomik), [@ChrisiSailer](https://github.com/ChrisiSailer), [@konradlang](https://github.com/konradlang)!*

**License:** GNU Affero General Public License v3.0  
**Developed by:** [BBMRI-ERIC](https://bbmri-eric.eu)

---

# Negotiator v3.20.0

**Release Date:** March 2026  
**Full Changelog:** [v3.19.0...v3.20.0](https://github.com/BBMRI-ERIC/negotiator/compare/v3.19.0...v3.20.0)

## What's New

### Interactive Guided Tours

![Interactive Guided Tour](/public/v3.20.0-interactive_guided_tour.gif)

We've introduced **interactive guided tours** to help you get started with the Negotiator more easily! These step-by-step tutorials walk you through key features of the platform, including:

- Navigation menu overview
- How to filter and sort negotiations
- Managing your negotiations
- Admin settings (for administrators)
- Governance page walkthrough

The tours appear automatically for new users and can be restarted anytime from the help menu.

### Custom Negotiation Display ID

![Negotiation ID and Search for title text](/public/v3.20.0-filter_and_negotiation_id.gif)

Negotiations now support a **customizable Display ID** that can be viewed and edited. This allows your organization to use your own reference numbers alongside the system-generated IDs, making it easier to track negotiations in your internal systems.

---

### Improved Email Notifications

#### Email Threading

Emails related to the same negotiation are now **grouped together** in your inbox. This makes it much easier to follow conversations and track the history of a negotiation without searching through scattered messages.

#### New Representative Notifications

When you are added as a representative to a resource, you will now receive a **welcome email notification** with links to all relevant negotiations. This ensures you never miss important access requests that need your attention.

### Better Network Experience

- **Draft negotiations** are now excluded from network listings and statistics, giving you a cleaner view of active negotiations
- Fixed an issue where resource representatives weren't displaying correctly for network organizations

## User Interface Improvements

### Consistent Design

We've refreshed the interface with a more consistent look and feel:

- Unified button styles across all pages
- Improved badge components for status indicators
- Better timestamp displays
- Cleaner table layouts with improved accessibility

### Search Improvements

- Representative search has been split into **two separate fields** for name and email, making it easier to find the right person
- Better handling when no search results are found

## For Administrators

### Customizable Appearance

- **Custom favicon**: Administrators can now set a custom browser tab icon through the UI settings

- **Open Sans font**: The interface now uses the Open Sans font for improved readability

### Email Configuration

- **Rate limiting**: New configurable parameters to control email sending frequency, helping prevent delivery issues with mail servers that have strict limits
- **Configurable delay**: Added delay between sending emails to improve deliverability

### Deployment Updates

- Updated Traefik configuration for improved routing
- HTTP request logging now available for better troubleshooting
- Updated Docker images to Node.js LTS 24.11.0

### Analytics

- Improved Matomo analytics integration
- Better cookie consent banner handling
- Fixed issues with local storage access when disabled

## Bug Fixes

- **PDF Generation**: Fixed an issue where form fields appeared in the wrong order in generated PDF documents
- **Access Forms**: Fixed missing elements when updating access forms
- **Form Display**: Fixed formatting of new lines in negotiation payload view
- **Login Page**: Improved error handling and display name fallbacks
- **Filter Tables**: Fixed an error when filtering negotiations with null values

## Dependency Updates

We've updated various components to their latest versions for improved security and performance, including Bootstrap 5.3.8, Vue 3.5, and multiple security patches.

*Thank you to all contributors who made this release possible!*

**License:** GNU Affero General Public License v3.0  
**Developed by:** [BBMRI-ERIC](https://bbmri-eric.eu)
