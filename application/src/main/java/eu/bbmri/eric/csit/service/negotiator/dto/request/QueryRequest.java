package eu.bbmri.eric.csit.service.negotiator.dto.request;

import java.util.Set;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

public class QueryRequest {

  @NotNull(message = "The url of the original query must be present")
  private String url;

  @NotNull(message = "A human readable description of the query must be present")
  private String humanReadable;

  @NotNull
  @NotEmpty(message = "At least one resource must be present")
  private Set<BiobankDTO> resources;

  public String getUrl() {
    return url;
  }

  public String getHumanReadable() {
    return humanReadable;
  }

  public Set<BiobankDTO> getResources() {
    return resources;
  }
}
