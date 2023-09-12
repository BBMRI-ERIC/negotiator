package eu.bbmri.eric.csit.service.negotiator.database.model;

import java.util.Objects;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.hibernate.annotations.GenericGenerator;

/** Class representing an Institution/Organization such as a Biobank. */
@Getter
@Entity
@Builder
@AllArgsConstructor
public class Organization {

  @Id
  @GeneratedValue(generator = "uuid")
  @GenericGenerator(name = "uuid", strategy = "uuid2")
  private final String id;

  private String name;

  @ManyToMany
  @JoinTable(
      name = "organization_resources_link",
      joinColumns = @JoinColumn(name = "organization_id"),
      inverseJoinColumns = @JoinColumn(name = "resource_id"))
  private Set<Resource> resources;

  protected Organization() {
    id = null;
  }

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
