package eu.bbmri.eric.csit.service.negotiator.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sun.istack.NotNull;
import eu.bbmri.eric.csit.service.model.DataSource.ApiType;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class DataSourceRequest {
  private String description;

  @NotNull private String name;

  @NotNull private String url;

  @NotNull private String apiUrl;

  @NotNull private String apiUsername;

  @NotNull private String apiPassword;

  @Enumerated(EnumType.STRING)
  @NotNull
  private ApiType apiType;

  @NotNull private String resourceNetwork;

  @NotNull private String resourceBiobank;

  @NotNull private String resourceCollection;

  @NotNull private Boolean syncActive;

  private Boolean sourcePrefix;
}
