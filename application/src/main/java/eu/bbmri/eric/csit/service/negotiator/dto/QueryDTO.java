package eu.bbmri.eric.csit.service.negotiator.dto;

import java.util.List;

public class QueryDTO {

  private String url;

  private String humanReadable;

  private List<CollectionDTO> collections;

  public String getUrl() {
    return url;
  }

  public String getHumanReadable() {
    return humanReadable;
  }

  public List<CollectionDTO> getCollections() {
    return collections;
  }
}
