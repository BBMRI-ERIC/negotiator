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
## Governance and Permissions

The Negotiator operates on a hierarchical governance structure designed to mirror real-world organizational relationships and resource management. Understanding this structure is crucial for proper system administration and ensuring smooth negotiation workflows.

### Governance Structure Overview

The system is built around two core entities:

1. **Organizations** - Institutional entities that own and manage resources
2. **Resources** - Specific datasets, biobanks, or research assets available for negotiation

Each Resource must belong to exactly one Organization, creating a clear ownership hierarchy that reflects real-world institutional structures.

### Accessing Governance Management

To manage the governance structure:

1. **Login as Administrator**  
   Ensure you have administrator privileges to access governance features.

2. **Navigate to Governance Page**  
   Access the governance management interface via:
   - **User Menu:** Click on your user menu and select **"Governance"**
   - **Direct URL:** Navigate to `/governance` in your browser

### Managing Organizations

Organizations serve as the top-level entities in the governance hierarchy. They represent institutions, companies, or research facilities that own resources.

#### Creating Organizations

1. In the Governance page, click **"Create Organization"**
2. Provide the following required information:
   - **Organization Name:** The full institutional name
   - **External ID:** A unique identifier used for external integrations
   - **Description:** A comprehensive description of the organization's purpose and scope
3. Optional information:
   - **Contact Email:** Primary contact for organizational matters
   - **Organization URI:** Official website or institutional page

#### Organization Management Features

- **Edit Organizations:** Update organizational details, contact information, or status
- **Withdraw Organizations:** Mark organizations as withdrawn while preserving historical data
- **Organization Status:** Track active vs. withdrawn organizations with visual indicators

### Managing Resources

Resources represent the actual datasets, biobanks, or research materials that can be negotiated for access. Each resource is owned by an organization and can have multiple representatives.

#### Creating Resources

1. Select an organization from the governance interface
2. Click **"Add Resources"** for the chosen organization
3. Complete the resource creation form:
   - **Name:** Descriptive name for the resource
   - **Source ID:** External identifier for the resource
   - **Description:** Detailed description of the resource contents and scope
   - **Access Form:** Select the appropriate access request form
   - **Discovery Service ID:** Numerical identifier for discovery service integration
   - **Contact Email:** (Optional) Resource-specific contact information
   - **Resource URI:** (Optional) Link to resource documentation or catalog entry

#### Resource Configuration

Resources require several technical configurations:

- **Access Forms:** Define the information collection process for access requests
- **Discovery Service Integration:** Enable resource discovery through external catalogs
- **Lifecycle Management:** Configure state transitions and approval workflows

### Resource Representatives

Representatives are the cornerstone of the negotiation process. They are responsible for reviewing access requests, mediating negotiations, and making decisions about resource access.

#### Understanding Representative Roles

Representatives serve multiple critical functions:

- **Request Review:** Evaluate incoming negotiation requests for their resources
- **Decision-Making:** Approve or deny access based on institutional policies
- **Communication:** Interact with researchers during the negotiation process
- **Compliance:** Ensure all access grants meet legal and ethical requirements

#### Managing Resource Representatives

Each resource can have multiple representatives to ensure coverage and expertise:

1. **Adding Representatives:**
   - Click the **people icon** next to any resource
   - Use the search function to find users by name or email
   - Select users from the search results to add as representatives
   - Review pending changes before saving

2. **Representative Assignment Process:**
   - Search and stage multiple users for addition
   - Remove existing representatives as needed
   - **Batch Processing:** All changes are applied together when you click "Save Changes"
   - **Visual Indicators:** New additions show with green "New" badges, removals show struck-through

3. **Representative Requirements:**
   - Users must have active accounts in the system
   - Representatives should have appropriate expertise for the resource domain
   - Consider assigning multiple representatives for redundancy and coverage

#### Visual Indicators for Resource Management

The governance interface provides clear visual feedback:

- **⚠️ Warning Triangle:** Appears when a resource has no assigned representatives
- **Representative Count:** Shows the number of current representatives per resource
- **Status Indicators:** Active vs. withdrawn resources are visually distinguished
- **Change Preview:** Pending representative changes are highlighted before confirmation

### Best Practices for Governance Management

#### Organizational Structure

- **Consistent Naming:** Use clear, standardized naming conventions for organizations
- **Complete Information:** Provide comprehensive descriptions and contact details
- **Regular Updates:** Keep organizational information current and accurate

#### Operational Considerations

- **Access Patterns:** Monitor which resources receive the most requests
- **Response Times:** Track representative response times to negotiation requests
- **Resource Utilization:** Review which resources are actively used vs. dormant
- **Compliance Monitoring:** Ensure all resource assignments meet institutional policies

### Troubleshooting Common Issues

#### Resources Without Representatives
- **Problem:** Resources showing warning triangles
- **Solution:** Assign at least one representative to each active resource
- **Prevention:** Regular audits of resource representative assignments

#### Organizational Hierarchy Issues
- **Problem:** Resources assigned to incorrect organizations
- **Solution:** Use the edit functionality to reassign resources to appropriate organizations
- **Prevention:** Careful review during initial resource creation

#### Representative Access Issues
- **Problem:** Representatives unable to access negotiation requests
- **Solution:** Verify user accounts are active and properly assigned to resources
- **Prevention:** Regular validation of user account status and permissions

This governance structure ensures that negotiation requests flow through proper institutional channels while maintaining clear accountability and decision-making authority.

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
The Negotiator provides a set of templates that can be customized to suit your needs. There are three types of templates
Email, PDF and Common templates.
Admin users can update or reset these templates via the UI in the Admin Settings -> Templates.
The templates are written in HTML with [Thymeleaf](https://www.thymeleaf.org/) syntax, allowing you to include dynamic
content using expressions, conditionals, and loops.

### Email Templates
The available variables are listed in the [Notification](notifications.md) documentation.

### PDF Templates
The available variables are:
- `negotiationPdfData.author.name`: The name of the author.
- `negotiationPdfData.author.email`: The email of the author.
- `negotiationPdfData.id`: The ID of the negotiation.
- `negotiationPdfData.createdAt`: The date the negotiation was created.
- `negotiationPdfData.status`: The status of the negotiation.
- `negotiationPdfData.payload`: The payload of the negotiation. This is in JSON format and is in the default templated 
  looped over to generate the individual sections of the PDF.
- `now`: The current date and time.
- `logoUrl`: The URL of the logo.
