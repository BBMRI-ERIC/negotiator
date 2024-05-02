package eu.bbmri_eric.negotiator.database.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import lombok.*;

@Getter
@Setter
@Entity
@Builder
public class Network {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "resource_id_seq")
  @SequenceGenerator(name = "resource_id_seq", initialValue = 10000, allocationSize = 1)
  private Long id;

  @NotNull private String uri;

  @Column(unique = true)
  private String name;

  /** A unique and persistent identifier issued by an appropriate institution. */
  @NotNull
  @Column(unique = true)
  private String externalId;

  private String contactEmail;

  @ManyToMany(mappedBy = "networks")
  private Set<Person> managers = new HashSet<>();

  @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  @JoinTable(
      name = "network_resources_link",
      joinColumns = @JoinColumn(name = "network_id"),
      inverseJoinColumns = @JoinColumn(name = "resource_id"))
  @Builder.Default
  private Set<Resource> resources = new HashSet<>();

  /** No-args constructor for JPA and other reflection-based tools. */
  Network() {}

  /**
   * All-args constructor for creating new instances.
   *
   * @param id the network's ID
   * @param uri the network's URI
   * @param name the network's name
   * @param externalId the network's external ID
   * @param contactEmail the network's contact email
   * @param managers the network's managers
   * @param resources the network's resources
   */
  Network(
      Long id,
      String uri,
      String name,
      String externalId,
      String contactEmail,
      Set<Person> managers,
      Set<Resource> resources) {
    this.id = id;
    this.uri = uri;
    this.name = name;
    this.externalId = externalId;
    this.contactEmail = contactEmail;
    this.managers = managers;
    this.resources = resources;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Network that = (Network) o;
    return Objects.equals(externalId, that.externalId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(externalId);
  }

  public void addResource(Resource collection) {
    resources.add(collection);
    collection.getNetworks().add(this);
  }

  public void removeResource(Resource collection) {
    resources.remove(collection);
    collection.getNetworks().remove(this);
  }
}
