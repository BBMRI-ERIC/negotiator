package eu.bbmri.eric.csit.service.negotiator.api.dto.datasource;

import eu.bbmri.eric.csit.service.negotiator.api.dto.ValidationGroups.Create;
import eu.bbmri.eric.csit.service.negotiator.database.model.DataSource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.validation.constraints.NotNull;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataSourceCreateDTO {

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

  private String sourcePrefix;
}
