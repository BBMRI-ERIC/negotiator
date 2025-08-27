## Customizable User Interface

The **Customizable UI** allows users to tailor the application’s appearance to fit their preferences or organizational
branding. From adjusting color schemes to rearranging components, the UI can be easily modified, providing a
personalized user experience that aligns with individual or company aesthetics.

---

### How to Customize the UI

To customize the UI, you need administrator privileges. Follow these steps:

#### Logging in as Admin and Accessing UI Configuration

1. **Login as Admin**  
   Log in to the system using your administrator credentials.

2. **Access the UI Configuration**  
   Choose one of the following methods:
    - **Via Dropdown Menu:**
        * After logging in, locate the dropdown menu.
        * Select the **Admin UI Configuration** option.
    - **Via Direct URL:**
        * Navigate to the following URL in your browser: _/ui-configuration_.

---

### Configuring the Image URL

The image URL input field can accept two types of values:

1. **Local File Name:**  
   Enter the name of a file available locally within the system.
    * Example: _bbmri_

2. **External Image URL:**  
   Provide the complete URL of an image hosted on an external server.
    * Example: `https://example.com/image.png`

## Additional Information Requirements Guide

The Negotiator allows you to define additional information requirements for Resource State Machine transitions available
to the Representatives.
This guide provides an overview of how to set up and manage these requirements.

### Setting Up Additional Information Requirements

You can do this by going to the Admin Settings -> Information Requirements section in the Negotiator UI.
There, you can add a new requirement by providing the following information:

- **Form**: The form that will be used to collect the additional information. You can choose from a list of available
  forms.
- **Lifecycle Event**: The lifecycle event that will trigger the additional information requirement. You can choose from
  a list of available events.
- **Administrator Summary**: If the summary of information supplied by all representatives is to be shown to the
  administrator or all participants.

After you have added a requirement, it will be displayed in the list of requirements.
The requirement will be triggered when a Representative tries to advance the State Machine.

## Webhooks

The Negotiator uses a webhook system to allow external systems to be notified of events that occur within the
Negotiator.
This guide provides an overview of how to set up and use webhooks with the Negotiator.

### Setting Up Webhooks

To set up webhooks, you need to provide a URL endpoint that the Negotiator can send HTTP POST requests to when certain
events occur.
You can do this by going to the Admin Settings -> Webhooks section in the Negotiator UI.
There, you can add a new webhook by providing the following information:

- **Webhook URL**: The URL endpoint that will receive the webhook notifications.
- **SSL Verification**: Whether to verify the SSL certificate of the webhook URL. This is enabled by default and it is
  recommended to keep it enabled for security reasons.
- **Active**: Whether the webhook is active or not. You can disable a webhook if you want to temporarily stop receiving
  notifications without deleting it.

> [!WARNING]  
> We currently do not support authentication for webhooks. This means that anyone who knows the webhook URL can send
> requests to it.
> Please ensure that your webhook URL is not publicly accessible or use a firewall to restrict access to it. If you
> would like us to implement authentication for webhooks, please let us know.

## Templates

The Negotiator provides a set of customizable templates used for generating PDFs, emails, and more. Admin users can
update or reset these templates via the UI in Admin Settings → Templates.

Templates are written in HTML using [Thymeleaf](https://www.thymeleaf.org/) syntax, allowing dynamic content through
expressions, conditionals, and loops.

> [!NOTE]
> If you customize a template, it will not be automatically updated when a new version of Negotiator is released. This
> ensures your customizations are preserved during upgrades.
> To restore a template to the latest official version, use the Reset option.

> [!WARNING]
> Resetting a template will remove all your customizations for that template.

Below is a table of all available variables for the email template:

| Variable Name           | Description                                                   |
|-------------------------|---------------------------------------------------------------|
| recipient               | The name of the recipient of the email                        |
| message                 | The main content or body of the message                       |
| negotiation             | The ID of the negotiation (if available)                      |
| titleForNegotiation     | The title of the negotiation (if available)                   |
| date                    | The formatted creation date of the negotiation (if available) |
| frontendUrl             | The base URL of the frontend application                      |
| emailYoursSincerelyText | The closing "Yours sincerely" text used in the email footer   |
| emailHelpdeskHref       | The URL (href) for the helpdesk or support                    |
| logoUrl                 | The URL to the logo displayed in the email                    |

Below is a table of all available variables for the PDF template:

| Variable Name           | Description                                                        |
|-------------------------|--------------------------------------------------------------------|
| now                     | The current timestamp                                              |
| logoUrl                 | The URL to the logo displayed in the document                      |
| authorName              | The name of the user who created the negotiation                   |
| authorEmail             | The email address of the negotiation's creator                     |
| negotiationId           | The unique identifier of the negotiation                           |
| negotiationTitle        | The title or subject of the negotiation                            |
| negotiationCreatedAt    | The creation date of the negotiation                               |
| negotiationStatus       | The current status or state of the negotiation                     |
| negotiationPayload      | The processed payload content related to the negotiation           |
| resourcesByOrganization | A mapping or grouping of resources categorized by organization     |
| totalResourceCount      | The total number of resources involved in the negotiation          |
| totalOrganizationCount  | The total number of organizations participating in the negotiation |
