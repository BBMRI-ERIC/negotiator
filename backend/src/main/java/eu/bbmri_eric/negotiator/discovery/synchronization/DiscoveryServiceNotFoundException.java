package eu.bbmri_eric.negotiator.discovery.synchronization;

import java.util.List;

public class DiscoveryServiceNotFoundException extends RuntimeException {

  private static final String errorMessage = "Discovery Service with id %s not found";

  public DiscoveryServiceNotFoundException(Long discoveryServiceId) {
    super(errorMessage.formatted(discoveryServiceId));
  }

  public DiscoveryServiceNotFoundException(List<Long> entityId) {
    super(errorMessage.formatted(entityId));
  }

  public DiscoveryServiceNotFoundException(String entityId) {
    super(errorMessage.formatted(entityId));
  }
}
