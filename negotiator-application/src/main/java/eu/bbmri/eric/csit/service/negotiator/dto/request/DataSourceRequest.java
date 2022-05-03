package eu.bbmri.eric.csit.service.negotiator.dto.request;

import eu.bbmri.eric.csit.service.negotiator.dto.ValidationGroups.Create;
import eu.bbmri.eric.csit.service.negotiator.model.DataSource;
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
  @NotNull(groups = Create.class)
  private String description;

  @NotNull(groups = Create.class)
  private String name;

  @NotNull(groups = Create.class)
  private String url;

  @NotNull(groups = Create.class)
  private String apiUrl;

  @NotNull(groups = Create.class)
  private String apiUsername;

  @NotNull(groups = Create.class)
  private String apiPassword;

  @Enumerated(EnumType.STRING)
  @NotNull(groups = Create.class)
  private DataSource.ApiType apiType;

  @NotNull(groups = Create.class)
  private String resourceNetwork;

  @NotNull(groups = Create.class)
  private String resourceBiobank;

  @NotNull(groups = Create.class)
  private String resourceCollection;

  @NotNull(groups = Create.class)
  private Boolean syncActive;

  private Boolean sourcePrefix;
}
