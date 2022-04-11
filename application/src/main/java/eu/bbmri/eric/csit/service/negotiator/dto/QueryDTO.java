package eu.bbmri.eric.csit.service.negotiator.dto;

import java.util.List;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

public class QueryDTO {

  @NotNull(message = "The url of the original query must be present")
  private String url;

  @NotNull(message = "A human readable description of the query must be present")
  private String humanReadable;

  @NotNull
  @NotEmpty(message = "At least one resource must be present")
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
