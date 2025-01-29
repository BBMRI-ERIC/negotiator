package eu.bbmri_eric.negotiator.governance.organization;

import eu.bbmri_eric.negotiator.common.AuditEntity;
import eu.bbmri_eric.negotiator.governance.resource.Resource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
public class Organization extends AuditEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "resource_id_seq")
  @SequenceGenerator(name = "resource_id_seq", initialValue = 10000, allocationSize = 1)
  private Long id;

  /** A unique and persistent identifier issued by an appropriate institution. */
  @NotNull
  @Column(unique = true)
  private String externalId;

  /** The name of the organization. */
  @NotNull private String name;

  /** The description of the organization. */
  @Column(columnDefinition = "VARCHAR(5000)")
  @NotNull
  private String description;

  /** The list of resources that are part of the organization. */
  @OneToMany(mappedBy = "organization")
  private Set<Resource> resources = new HashSet<>();

  /** The contact email of the organization. */
  private String contactEmail;

  /** Flag indicating if the organization is withdrawn or not. */
  @NotNull @Builder.Default private Boolean withdrawn = false;

  /** URI of the organization. */
  private String uri;

  public Organization(String externalId, String name) {
    this.externalId = externalId;
    this.name = name;
  }

  public Organization(String externalId, String name, String description, Set<Resource> resources) {
    this.externalId = externalId;
    this.name = name;
    this.description = description;
    this.resources = resources;
  }

  public Organization(
      String externalId,
      String name,
      String description,
      Set<Resource> resources,
      Boolean withdrawn) {
    this.externalId = externalId;
    this.name = name;
    this.description = description;
    this.resources = resources;
    this.withdrawn = withdrawn;
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
