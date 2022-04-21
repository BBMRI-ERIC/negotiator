package eu.bbmri.eric.csit.service.negotiator.dto.request;

import eu.bbmri.eric.csit.service.model.DataSource.ApiType;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataSourceRequest {
  @NotNull private String description;

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
