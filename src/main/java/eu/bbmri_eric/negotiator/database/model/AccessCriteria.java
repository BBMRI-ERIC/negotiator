package eu.bbmri_eric.negotiator.database.model;

import com.sun.istack.NotNull;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
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
public class AccessCriteria extends BaseEntity implements Comparable<AccessCriteria> {

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
