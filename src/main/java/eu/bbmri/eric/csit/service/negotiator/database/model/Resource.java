package eu.bbmri.eric.csit.service.negotiator.database.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;
import java.util.Set;
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
@Table(name = "resource")
public class Resource {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "resource_id_seq")
  @SequenceGenerator(name = "resource_id_seq", initialValue = 10000, allocationSize = 1)
  private Long id;

  private String name;

  @Column(columnDefinition = "VARCHAR(5000)")
  private String description;

  @NotNull private String sourceId;

  @ManyToMany
  @JoinTable(
      name = "resource_representative_link",
      joinColumns = @JoinColumn(name = "resource_id"),
      inverseJoinColumns = @JoinColumn(name = "person_id"))
  @Exclude
  private Set<Person> representatives;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "data_source_id")
  @Exclude
  @JsonIgnore
  @NotNull
  private DataSource dataSource;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "organization_id")
  @Exclude
  @NotNull
  private Organization organization;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "access_criteria_set_id")
  @Exclude
  private AccessCriteriaSet accessCriteriaSet;

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
