package eu.bbmri_eric.negotiator.service;

public interface DiscoveryServiceClient {

  public void syncAllOrganizations();

  public void syncAllResources();

  public void syncAllNetworks();
}
