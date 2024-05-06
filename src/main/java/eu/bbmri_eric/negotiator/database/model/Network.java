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
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Network {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "network_id_seq")
  @SequenceGenerator(name = "network_id_seq", initialValue = 10000, allocationSize = 1)
  private Long id;

  /** The URI of the network */
  @NotNull private String uri;

  /** The name of the network */
  @Column(unique = true)
  private String name;

  /** A unique and persistent identifier issued by an appropriate institution */
  @NotNull
  @Column(unique = true)
  private String externalId;

  /** The contact email of the network. */
  private String contactEmail;

  /** The managers of the network. */
  @ManyToMany(mappedBy = "networks")
  private Set<Person> managers = new HashSet<>();

  /** The resources of the network. */
  @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  @JoinTable(
      name = "network_resources_link",
      joinColumns = @JoinColumn(name = "network_id"),
      inverseJoinColumns = @JoinColumn(name = "resource_id"))
  @Builder.Default
  private Set<Resource> resources = new HashSet<>();

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
}
