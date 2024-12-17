package eu.bbmri_eric.negotiator.governance.network;

import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.user.Person;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.validation.constraints.NotNull;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString.Exclude;

/** Represents a Network entity in the database. */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Network {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "network_id_seq")
  @SequenceGenerator(name = "network_id_seq", initialValue = 10000, allocationSize = 1)
  private Long id;

  /** The URI of the network */
  @NotNull private String uri;

  /** The name of the network */
  @Column private String name;

  /** The description of the network */
  @Column(columnDefinition = "VARCHAR(5000)")
  private String description;

  /** A unique and persistent identifier issued by an appropriate institution */
  @NotNull
  @Column(unique = true)
  private String externalId;

  /** The contact email of the network. */
  private String contactEmail;

  /** The managers of the network. */
  @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  @JoinTable(
      name = "network_person_link",
      joinColumns = @JoinColumn(name = "network_id"),
      inverseJoinColumns = @JoinColumn(name = "person_id"))
  @Exclude
  @Setter(AccessLevel.NONE)
  @Builder.Default
  private Set<Person> managers = new HashSet<>();

  /** The resources of the network. */
  @ManyToMany(
      cascade = {CascadeType.PERSIST, CascadeType.MERGE},
      fetch = FetchType.LAZY)
  @JoinTable(
      name = "network_resources_link",
      joinColumns = @JoinColumn(name = "network_id"),
      inverseJoinColumns = @JoinColumn(name = "resource_id"))
  @Builder.Default
  @Exclude
  @Setter(AccessLevel.NONE)
  private Set<Resource> resources = new HashSet<>();

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Network that = (Network) o;
    return Objects.equals(externalId, that.externalId)
        && Objects.equals(id, that.id)
        && Objects.equals(uri, that.uri)
        && Objects.equals(name, that.name)
        && Objects.equals(contactEmail, that.contactEmail);
  }

  @Override
  public int hashCode() {
    return Objects.hash(externalId, id, uri, name, contactEmail);
  }

  /** Adds a resource to the network. */
  public void addResource(Resource collection) {
    resources.add(collection);
    collection.getNetworks().add(this);
  }

  /** Removes a resource from the network. */
  public void removeResource(Resource collection) {
    resources.remove(collection);
    collection.getNetworks().remove(this);
  }

  /** Returns all resources in the network. */
  public Set<Resource> getResources() {
    if (Objects.isNull(this.resources)) {
      return Set.of();
    }
    return Collections.unmodifiableSet(this.resources);
  }

  /** Adds a manager to the network. */
  public void addManager(Person manager) {
    managers.add(manager);
    manager.getNetworks().add(this);
  }

  /** Removes a manager from the network. */
  public void removeManager(Person manager) {
    managers.remove(manager);
    manager.getNetworks().remove(this);
  }

  /** Returns all managers of the network. */
  public Set<Person> getManagers() {
    if (Objects.isNull(this.managers)) {
      return Set.of();
    }
    return Collections.unmodifiableSet(this.managers);
  }
}
