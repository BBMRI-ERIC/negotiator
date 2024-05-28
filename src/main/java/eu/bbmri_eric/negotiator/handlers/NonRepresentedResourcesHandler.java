package eu.bbmri_eric.negotiator.handlers;

/** Handler for non-represented resources. */
public interface NonRepresentedResourcesHandler {
  /**
   * Update the status of a resource in old negotiations where it is marked as unreachable.
   *
   * @param resourceId the id of the resource to update
   */
  void updateResourceInOngoingNegotiations(Long resourceId, String sourceId);
}
