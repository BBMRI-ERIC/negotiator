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
@NoArgsConstructor
@AllArgsConstructor
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

  @OneToMany(mappedBy = "network", cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
  private Set<Person> managers;

  @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  @JoinTable(
      name = "network_resources_link",
      joinColumns = @JoinColumn(name = "network_id"),
      inverseJoinColumns = @JoinColumn(name = "resource_id"))
  @ToString.Exclude
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

  public void addResource(Resource collection) {
    resources.add(collection);
    collection.getNetworks().add(this);
  }
}
