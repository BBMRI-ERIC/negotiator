package eu.bbmri.eric.csit.service.model;

import java.util.Objects;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@ToString
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Entity(name = "DataSource")
@Table(name = "data_source")
public class DataSource extends BaseEntity {

  public enum ApiType {
    MOLGENIS
  }

  private String description;

  @NotNull
  private String name;

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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DataSource that = (DataSource) o;
    return Objects.equals(getDescription(), that.getDescription())
        && Objects.equals(getName(), that.getName()) && Objects.equals(getUrl(),
        that.getUrl()) && Objects.equals(getApiUrl(), that.getApiUrl())
        && Objects.equals(getApiUsername(), that.getApiUsername())
        && Objects.equals(getApiPassword(), that.getApiPassword())
        && getApiType() == that.getApiType() && Objects.equals(getResourceNetwork(),
        that.getResourceNetwork()) && Objects.equals(getResourceBiobank(),
        that.getResourceBiobank()) && Objects.equals(getResourceCollection(),
        that.getResourceCollection()) && Objects.equals(getSyncActive(),
        that.getSyncActive()) && Objects.equals(getSourcePrefix(), that.getSourcePrefix());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getDescription(), getName(), getUrl(), getApiUrl(), getApiUsername(),
        getApiPassword(), getApiType(), getResourceNetwork(), getResourceBiobank(),
        getResourceCollection(), getSyncActive(), getSourcePrefix());
  }
}
