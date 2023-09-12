package eu.bbmri.eric.csit.service.negotiator.database.model;

import java.util.Objects;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/** Class representing an Institution/Organization such as a Biobank. */
@Getter
@Setter
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class Organization {

  @NonNull private final String id;

  private String name;

  private Set<Resource> resources;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Organization that = (Organization) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
