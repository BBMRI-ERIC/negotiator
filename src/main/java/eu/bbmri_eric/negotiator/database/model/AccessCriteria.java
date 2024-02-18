package eu.bbmri_eric.negotiator.database.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.ToString.Exclude;

@ToString
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class AccessCriteria extends AuditEntity implements Comparable<AccessCriteria> {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @NotNull private String name;

  @NotNull private String label;

  @NotNull private String description;

  @NotNull private String type;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "access_criteria_section_id")
  @Exclude
  private AccessCriteriaSection section;

  @OneToMany(mappedBy = "accessCriteria")
  @Exclude
  private Set<AccessCriteriaSectionLink> accessCriteriaSectionLinks;

  @Override
  public int compareTo(AccessCriteria section) {
    if (this.getId() < section.getId()) {
      return -1;
    } else if (this.getId().equals(section.getId())) {
      return 0;
    } else {
      return 1;
    }
  }
}