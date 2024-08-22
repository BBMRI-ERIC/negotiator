package eu.bbmri_eric.negotiator.database.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.ToString.Exclude;

@ToString
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Entity(name = "Resource")
public class Resource {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "resource_id_seq")
  @SequenceGenerator(name = "resource_id_seq", initialValue = 10000, allocationSize = 1)
  private Long id;

  private String name;

  @Column(columnDefinition = "VARCHAR(5000)")
  private String description;

  @NotNull private String sourceId;

  @ManyToMany(
      fetch = FetchType.LAZY,
      cascade = {CascadeType.PERSIST, CascadeType.MERGE},
      mappedBy = "resources")
  @JsonIgnore
  @Exclude
  private Set<Person> representatives = new HashSet<>();

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "discovery_service_id")
  @Exclude
  @JsonIgnore
  @NotNull
  private DiscoveryService discoveryService;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "organization_id")
  @Exclude
  @NotNull
  private Organization organization;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "access_form_id")
  @Exclude
  private AccessForm accessForm;

  @ManyToMany(mappedBy = "resources")
  @Builder.Default
  @Setter(AccessLevel.NONE)
  private Set<Network> networks = new HashSet<>();

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Resource resource = (Resource) o;
    return Objects.equals(sourceId, resource.sourceId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sourceId);
  }
}
