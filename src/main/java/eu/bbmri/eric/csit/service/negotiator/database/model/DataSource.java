package eu.bbmri.eric.csit.service.negotiator.database.model;

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

  private String description;
  @NotNull private String name;
  @NotNull private String apiUrl;
  @NotNull private String apiUsername;
  @NotNull private String apiPassword;

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
        && Objects.equals(getApiUrl(), that.getApiUrl())
        && Objects.equals(getApiUsername(), that.getApiUsername())
        && Objects.equals(getApiPassword(), that.getApiPassword())
        ;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        getDescription(),
        getName(),
        getApiUrl(),
        getApiUsername(),
        getApiPassword()
        );
  }

  public enum ApiType {
    MOLGENIS
  }
}
