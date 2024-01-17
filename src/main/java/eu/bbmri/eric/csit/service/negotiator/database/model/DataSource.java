package eu.bbmri.eric.csit.service.negotiator.database.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;
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
public class DataSource {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  private String description;

  @NotNull private String name;

  @NotNull
  @Column(unique = true)
  private String url;

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

  private String sourcePrefix;

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
        && Objects.equals(getName(), that.getName())
        && Objects.equals(getUrl(), that.getUrl())
        && Objects.equals(getApiUrl(), that.getApiUrl())
        && Objects.equals(getApiUsername(), that.getApiUsername())
        && Objects.equals(getApiPassword(), that.getApiPassword())
        && getApiType() == that.getApiType()
        && Objects.equals(getResourceNetwork(), that.getResourceNetwork())
        && Objects.equals(getResourceBiobank(), that.getResourceBiobank())
        && Objects.equals(getResourceCollection(), that.getResourceCollection())
        && Objects.equals(getSyncActive(), that.getSyncActive())
        && Objects.equals(getSourcePrefix(), that.getSourcePrefix());
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        getDescription(),
        getName(),
        getUrl(),
        getApiUrl(),
        getApiUsername(),
        getApiPassword(),
        getApiType(),
        getResourceNetwork(),
        getResourceBiobank(),
        getResourceCollection(),
        getSyncActive(),
        getSourcePrefix());
  }

  public enum ApiType {
    MOLGENIS
  }
}
