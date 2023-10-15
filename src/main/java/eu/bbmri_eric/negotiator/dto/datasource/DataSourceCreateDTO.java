package eu.bbmri_eric.negotiator.dto.datasource;

import eu.bbmri_eric.negotiator.database.model.DataSource;
import eu.bbmri_eric.negotiator.dto.ValidationGroups;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotNull;
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
