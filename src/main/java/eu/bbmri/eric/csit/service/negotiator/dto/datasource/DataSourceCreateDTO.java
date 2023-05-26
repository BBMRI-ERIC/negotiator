package eu.bbmri.eric.csit.service.negotiator.dto.datasource;

import eu.bbmri.eric.csit.service.negotiator.database.model.DataSource;
import eu.bbmri.eric.csit.service.negotiator.dto.ValidationGroups;
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

  @NotNull(groups = ValidationGroups.Create.class)
  private String description;

  @NotNull(groups = ValidationGroups.Create.class)
  private String name;

  @NotNull(groups = ValidationGroups.Create.class)
  private String url;

  @NotNull(groups = ValidationGroups.Create.class)
  private String apiUrl;

  @NotNull(groups = ValidationGroups.Create.class)
  private String apiUsername;

  @NotNull(groups = ValidationGroups.Create.class)
  private String apiPassword;

  @Enumerated(EnumType.STRING)
  @NotNull(groups = ValidationGroups.Create.class)
  private DataSource.ApiType apiType;

  @NotNull(groups = ValidationGroups.Create.class)
  private String resourceNetwork;

  @NotNull(groups = ValidationGroups.Create.class)
  private String resourceBiobank;

  @NotNull(groups = ValidationGroups.Create.class)
  private String resourceCollection;

  @NotNull(groups = ValidationGroups.Create.class)
  private Boolean syncActive;

  private String sourcePrefix;
}
