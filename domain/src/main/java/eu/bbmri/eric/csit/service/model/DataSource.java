package eu.bbmri.eric.csit.service.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity(name = "DataSource")
@Table(name = "data_source")
public class DataSource extends BaseEntity {

  private enum ApiType {
    MOLGENIS
  }

  @JsonIgnore private String description;

  @NotNull private String name;

  @NotNull private String url;

  @NotNull @JsonIgnore private String apiUrl;

  @NotNull @JsonIgnore private String apiUsername;

  @NotNull @JsonIgnore private String apiPassword;

  @Enumerated(EnumType.STRING)
  @NotNull
  @JsonIgnore
  private ApiType apiType;

  @NotNull @JsonIgnore private String resourceNetwork;

  @NotNull @JsonIgnore private String resourceBiobank;

  @NotNull @JsonIgnore private String resourceCollection;

  @NotNull @JsonIgnore private Boolean syncActive;

  @JsonIgnore private Boolean sourcePrefix;
}
