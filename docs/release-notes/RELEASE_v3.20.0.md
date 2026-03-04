# Negotiator v3.20.0

**Release Date:** March 2026  
**Full Changelog:** [v3.19.0...v3.20.0](https://github.com/BBMRI-ERIC/negotiator/compare/v3.19.0...v3.20.0)

---

## What's New

### Interactive Guided Tours

![Interactive Guided Tour](/docs/release-notes/3.20.0_interactive_guided_tour.gif)

We've introduced **interactive guided tours** to help you get started with the Negotiator more easily! These step-by-step tutorials walk you through key features of the platform, including:

- Navigation menu overview
- How to filter and sort negotiations
- Managing your negotiations
- Admin settings (for administrators)
- Governance page walkthrough

The tours appear automatically for new users and can be restarted anytime from the help menu.

---

### Custom Negotiation Display ID

![Negotiation ID and Search for title text](/docs/release-notes/v3.20.0-Filter_and_Negotiation_ID.gif)

Negotiations now support a **customizable Display ID** that can be viewed and edited. This allows your organization to use your own reference numbers alongside the system-generated IDs, making it easier to track negotiations in your internal systems.

---

### Improved Email Notifications

#### Email Threading

Emails related to the same negotiation are now **grouped together** in your inbox. This makes it much easier to follow conversations and track the history of a negotiation without searching through scattered messages.

#### New Representative Notifications

When you are added as a representative to a resource, you will now receive a **welcome email notification** with links to all relevant negotiations. This ensures you never miss important access requests that need your attention.

---

### Better Network Experience

- **Draft negotiations** are now excluded from network listings and statistics, giving you a cleaner view of active negotiations
- Fixed an issue where resource representatives weren't displaying correctly for network organizations

---

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

---

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

---

## Bug Fixes

- **PDF Generation**: Fixed an issue where form fields appeared in the wrong order in generated PDF documents
- **Access Forms**: Fixed missing elements when updating access forms
- **Form Display**: Fixed formatting of new lines in negotiation payload view
- **Login Page**: Improved error handling and display name fallbacks
- **Filter Tables**: Fixed an error when filtering negotiations with null values

---

## Dependency Updates

We've updated various components to their latest versions for improved security and performance, including Bootstrap 5.3.8, Vue 3.5, and multiple security patches.

---

*Thank you to all contributors who made this release possible!*

**License:** GNU Affero General Public License v3.0  
**Developed by:** [BBMRI-ERIC](https://bbmri-eric.eu)
