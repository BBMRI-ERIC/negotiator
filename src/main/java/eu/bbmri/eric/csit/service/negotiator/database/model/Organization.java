package eu.bbmri.eric.csit.service.negotiator.database.model;

import java.util.Objects;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/** Class representing an Institution/Organization such as a Biobank. */
@Getter
@Setter
@Builder
@AllArgsConstructor
public class Organization {
  @NotNull private String Id;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Organization that = (Organization) o;
    return Objects.equals(Id, that.Id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(Id);
  }
}
