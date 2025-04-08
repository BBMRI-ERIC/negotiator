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

