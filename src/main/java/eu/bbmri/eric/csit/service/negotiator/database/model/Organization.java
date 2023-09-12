package eu.bbmri.eric.csit.service.negotiator.database.model;

import java.util.Objects;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

/** Class representing an Institution/Organization such as a Biobank. */
@Getter
@Entity
@Builder
@AllArgsConstructor
public class Organization extends BaseEntity {
  /** A unique and persistent identifier issued by an appropriate institution. */
  @NonNull @NotNull private final String externalId;

  private String name;

  @ManyToMany
  @JoinTable(
      name = "organization_resources_link",
      joinColumns = @JoinColumn(name = "organization_id"),
      inverseJoinColumns = @JoinColumn(name = "resource_id"))
  private Set<Resource> resources;

  protected Organization() {
    externalId = null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Organization that = (Organization) o;
    return Objects.equals(externalId, that.externalId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(externalId);
  }
}
