## Customizable User Interface

The **Customizable UI** allows users to tailor the application's appearance to fit their preferences or organizational
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

The Negotiator operates on a hierarchical governance structure designed to mirror real-world organizational
relationships and resource management. Understanding this structure is crucial for proper system administration and
ensuring smooth negotiation workflows.

> [!IMPORTANT]
> The governance structure directly impacts user permissions and access rights throughout the system. Changes should be
> made carefully and with proper planning.

### Governance Structure Overview

The system is built around two core entities:

1. **Organizations** - Institutional entities that own and manage resources
2. **Resources** - Specific datasets, biobanks, or research assets available for negotiation

Each Resource must belong to exactly one Organization, creating a clear ownership hierarchy that reflects real-world
institutional structures.

> [!TIP]
> Map your governance structure before implementation to ensure it accurately reflects your organizational hierarchy and
> resource ownership.

### Accessing Governance Management

> [!WARNING]
> Only users with administrator privileges can access governance management features. Unauthorized access attempts will
> be logged.

To manage the governance structure:

1. **Login as Administrator**  
   Ensure you have administrator privileges to access governance features.

2. **Navigate to Governance Page**  
   Access the governance management interface via:
    - **User Menu:** Click on your user menu and select **"Governance"**
    - **Direct URL:** Navigate to `/governance` in your browser

> [!NOTE]
> The governance interface may take a few moments to load if you have a large number of organizations and resources in
> your system.

### Managing Organizations

Organizations serve as the top-level entities in the governance hierarchy. They represent institutions, companies, or
research facilities that own resources.

> [!TIP]
> Use descriptive organization names and maintain consistent naming conventions to make governance management easier.

#### Creating Organizations

> [!WARNING]
> Once created, organization External IDs cannot be changed. Ensure accuracy before saving.

1. In the Governance page, click **"Create Organization"**
2. Provide the following required information:
    - **Organization Name:** The full institutional name
    - **External ID:** A unique identifier used for external integrations
    - **Description:** A comprehensive description of the organization's purpose and scope
3. Optional information:
    - **Contact Email:** Primary contact for organizational matters
    - **Organization URI:** Official website or institutional page

> [!IMPORTANT]
> The External ID must be unique across the entire system. Consider using standardized organizational codes or acronyms.

> [!TIP]
> Include detailed descriptions to help users understand each organization's role and capabilities in the negotiation
> process.

#### Organization Management Features

- **Edit Organizations:** Update organizational details, contact information, or status
- **Withdraw Organizations:** Mark organizations as withdrawn while preserving historical data
- **Organization Status:** Track active vs. withdrawn organizations with visual indicators

> [!WARNING]
> Withdrawing an organization will affect all associated resources and ongoing negotiations. Review all dependencies
> before proceeding.

> [!NOTE]
> Withdrawn organizations are preserved for historical data integrity but cannot participate in new negotiations.

### Managing Resources

Resources represent the actual datasets, biobanks, or research materials that can be negotiated for access. Each
resource is owned by an organization and can have multiple representatives.

> [!TIP]
> Regularly review and update resource information to ensure accuracy and relevance for researchers.

#### Creating Resources

> [!IMPORTANT]
> Ensure the parent organization exists before creating resources. Resources cannot exist without an organizational
> owner.

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

> [!WARNING]
> Source IDs should be unique within the organization to prevent conflicts during external integrations.

> [!TIP]
> Use clear, descriptive resource names that help researchers quickly identify relevant datasets or materials.

#### Resource Configuration

Resources require several technical configurations:

- **Access Forms:** Define the information collection process for access requests
- **Discovery Service Integration:** Enable resource discovery through external catalogs
- **Lifecycle Management:** Configure state transitions and approval workflows

> [!NOTE]
> Access forms should be configured before making resources available for negotiation to ensure proper data collection.

### Resource Representatives

Representatives are the cornerstone of the negotiation process. They are responsible for reviewing access requests,
mediating negotiations, and making decisions about resource access.

> [!IMPORTANT]
> Every resource must have at least one representative to handle negotiation requests. Resources without representatives
> will show warning indicators.

#### Understanding Representative Roles

Representatives serve multiple critical functions:

- **Request Review:** Evaluate incoming negotiation requests for their resources
- **Decision-Making:** Approve or deny access based on institutional policies
- **Communication:** Interact with researchers during the negotiation process
- **Compliance:** Ensure all access grants meet legal and ethical requirements

> [!TIP]
> Assign multiple representatives per resource to ensure coverage during vacations, leave, or staff changes.

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

> [!WARNING]
> Removing all representatives from a resource will prevent new negotiations from being processed. Ensure at least one
> representative remains assigned.

#### Visual Indicators for Resource Management

The governance interface provides clear visual feedback:

- **⚠️ Warning Triangle:** Appears when a resource has no assigned representatives
- **Representative Count:** Shows the number of current representatives per resource
- **Status Indicators:** Active vs. withdrawn resources are visually distinguished
- **Change Preview:** Pending representative changes are highlighted before confirmation

> [!TIP]
> Use the warning triangles as a quick visual audit tool to identify resources that need representative assignment.

### Best Practices for Governance Management

#### Organizational Structure

- **Consistent Naming:** Use clear, standardized naming conventions for organizations
- **Complete Information:** Provide comprehensive descriptions and contact details
- **Regular Updates:** Keep organizational information current and accurate

> [!TIP]
> Establish naming conventions early and document them for consistent application across all organizations and
> resources.

#### Operational Considerations

- **Access Patterns:** Monitor which resources receive the most requests
- **Response Times:** Track representative response times to negotiation requests
- **Resource Utilization:** Review which resources are actively used vs. dormant
- **Compliance Monitoring:** Ensure all resource assignments meet institutional policies

> [!NOTE]
> Regular audits and reviews of the governance structure and resource assignments can help maintain an efficient and
> compliant negotiation environment.

### Troubleshooting Common Issues

#### Resources Without Representatives

- **Problem:** Resources showing warning triangles
- **Solution:** Assign at least one representative to each active resource
- **Prevention:** Regular audits of resource representative assignments

> [!TIP]
> Set up regular monthly reviews to check for resources without representatives and address them proactively.

#### Organizational Hierarchy Issues

- **Problem:** Resources assigned to incorrect organizations
- **Solution:** Use the edit functionality to reassign resources to appropriate organizations
- **Prevention:** Careful review during initial resource creation

> [!WARNING]
> Reassigning resources between organizations may affect ongoing negotiations and representative assignments.

#### Representative Access Issues

- **Problem:** Representatives unable to access negotiation requests
- **Solution:** Verify user accounts are active and properly assigned to resources
- **Prevention:** Regular validation of user account status and permissions

> [!IMPORTANT]
> Representative access issues can delay negotiations significantly. Monitor and resolve quickly to maintain service
> levels.
