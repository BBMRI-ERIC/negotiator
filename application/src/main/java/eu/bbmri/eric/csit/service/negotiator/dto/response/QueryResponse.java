package eu.bbmri.eric.csit.service.negotiator.dto.response;

import eu.bbmri.eric.csit.service.negotiator.dto.request.BiobankDTO;
import java.util.Set;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

public class QueryResponse {

  @NotNull private Long id;

  @NotNull(message = "The url of the original query must be present")
  private String url;

  @NotNull(message = "A human readable description of the query must be present")
  private String humanReadable;

  @NotNull
  @NotEmpty(message = "At least one resource must be present")
  private Set<BiobankDTO> resources;

  @NotNull private String queryToken;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getHumanReadable() {
    return humanReadable;
  }

  public void setHumanReadable(String humanReadable) {
    this.humanReadable = humanReadable;
  }

  public Set<BiobankDTO> getResources() {
    return resources;
  }

  public void setResources(Set<BiobankDTO> resources) {
    this.resources = resources;
  }

  public String getQueryToken() {
    return queryToken;
  }

  public void setQueryToken(String queryToken) {
    this.queryToken = queryToken;
  }
}
