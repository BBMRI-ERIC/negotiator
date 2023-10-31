package eu.bbmri.eric.csit.service.negotiator.database.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;
import java.util.Set;
import lombok.*;

/** Class representing an Institution/Organization such as a Biobank. */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@NamedEntityGraph(
    name = "organization-with-detailed-resources",
    attributeNodes = {@NamedAttributeNode(value = "resources")})
public class Organization extends BaseEntity {
  /** A unique and persistent identifier issued by an appropriate institution. */
  @NotNull
  @Column(unique = true)
  private String externalId;

  private String name;

  @OneToMany(mappedBy = "organization", cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
  private Set<Resource> resources;

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
