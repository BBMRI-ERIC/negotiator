# Collaborators

A collaborator has access to a negotiation and can view and edit it.

Only the negotiation author can decide who works on it: adding collaborators, removing them, and
transferring the negotiation to another user are all reserved for the author. A collaborator may
remove themselves from a negotiation at any time.

## Add Collaborators

The add collaborator feature allows you to add a user to a negotiation as a collaborator by entering their Subject ID.

### How to Add a Collaborator

1. **Find the Negotiation**: Locate the negotiation in the sidebar or list.
2. **Open the Modal**:
    - Click the **Add Collaborator** button (person icon with “Add Collaborator ” label). The
      button is only shown to the negotiation author.
3. **Enter Subject ID**:
    - Type the new collaborator’s Subject ID (e.g., `1001@bbmri.eu`) in the **Subject ID** field.
    - Find the ID on the user’s profile page.
    - Press **Enter** or click **Confirm** to submit.
4. **Review the Outcome**:
    - **Success**: A green message shows, e.g., “The user has been added as a collaborator!”.
    - **Error**: A red error message appears, e.g., “Negotiation or user not found.” Correct the ID and
      retry.
    - Common errors:
        - “Invalid Subject ID” (wrong or non-existent ID)
        - “Not authorized” (lacking permission)
        - “Negotiation or user not found”
        - “Network error” (connection issue)

### Tips

- **Verify ID**: Check the Subject ID on the user’s profile to avoid errors.
- **Permissions**: Ensure you have the required permissions; contact an admin if denied.
- **Connection**: Retry if a network error occurs.

## Remove Collaborators

The remove collaborator feature allows you to remove a collaborator from a negotiation by selecting them from the collaborators
list in the negotiation sidebar. After a successful removal, the collaborator will lose access to the negotiation.
**Note:** The negotiation author can remove any collaborator. A collaborator can only remove
themselves, which they do with the same button on their own entry in the list.

### How to Remove a Collaborator

1. **Find the Negotiation**: Locate the negotiation in the sidebar or list.
2. **Open the Modal**:
    - Click the **Remove Collaborator** button (red person icon next to the collaborator in the collaborator list).
3. **Confirm Removal**:
    - Check the confirmation dialog and click **Remove** to remove the collaborator.
4. **Review the Outcome**:
    - **Success**: A message shows, e.g., “[username] has been removed as a collaborator.”.

### Tips

- **Permissions**: Ensure you have the required permissions; contact an admin if denied.
- **Connection**: Retry if a network error occurs.

