package eu.bbmri_eric.negotiator.database.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
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

  @OneToMany(mappedBy = "network", cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
  private Set<Organization> members;

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
}
