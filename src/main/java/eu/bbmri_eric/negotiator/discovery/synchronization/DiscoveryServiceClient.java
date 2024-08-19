package eu.bbmri_eric.negotiator.discovery.synchronization;

public interface DiscoveryServiceClient {

  /** Synchronizes all the Organizations of the Discovery Service with the Negotiator. */
  void syncAllOrganizations();

  /** Synchronizes all the Resources of the Discovery Service with the Negotiator. */
  void syncAllResources();

  /** Synchronizes all the Networks of the Discovery Service with the Negotiator. */
  void syncAllNetworks();
}
