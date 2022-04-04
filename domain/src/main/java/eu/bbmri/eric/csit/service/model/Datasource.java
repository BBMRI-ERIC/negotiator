package eu.bbmri.eric.csit.service.model;

import com.sun.istack.NotNull;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@ToString
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "data_source")
public class Datasource extends BaseEntity {

  private enum ApiType {
    MOLGENIS
  }

  @NotNull private String name;

  private String description;

  @NotNull private String URL;

  @NotNull private String apiUrl;

  @NotNull private String apiUsername;

  @NotNull private String apiPassword;

  @Enumerated(EnumType.STRING)
  @NotNull
  private ApiType apiType;

  private String resourceNetwork;

  private String resourceBiobank;

  private String resourceCollection;

  @NotNull private Boolean syncActive;

  private Boolean sourcePrefix;
}
