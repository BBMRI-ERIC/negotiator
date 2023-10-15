package eu.bbmri_eric.negotiator.database.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

/** Class representing an Institution/Organization such as a Biobank. */
@Getter
@Entity
@Builder
@AllArgsConstructor
@NamedEntityGraph(
    name = "organization-with-detailed-resources",
    attributeNodes = {@NamedAttributeNode(value = "resources")})
public class Organization extends BaseEntity {
  /** A unique and persistent identifier issued by an appropriate institution. */
  @NonNull @NotNull private final String externalId;

  private String name;

  @OneToMany(mappedBy = "organization", cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
  private Set<Resource> resources;

  // For JPA compatability
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
